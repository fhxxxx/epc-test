package com.envision.epc.module.taxledger.application.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.facade.platform.PlatformRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.DatalakeExportAssembler;
import com.envision.epc.module.taxledger.application.command.DataLakePullCommand;
import com.envision.epc.module.taxledger.application.constant.DataLakeConstants;
import com.envision.epc.module.taxledger.application.dto.DataLakeBatchPullResultDTO;
import com.envision.epc.module.taxledger.application.dto.DatalakeDTO;
import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.dto.DlInputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOutputParsedDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Datalake pull and category split service.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataLakeService {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String ACCOUNT_INTEREST = "6603020011";
    private static final String ACCOUNT_OTHER_INCOME = "6702000010";
    private static final String ACCOUNT_INPUT_TRANSFER_OUT = "2221010400";

    private final PlatformRemote platformRemote;
    private final BlobStorageRemote blobStorageRemote;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final FileParseOrchestratorService fileParseOrchestratorService;
    private final DatalakeExportAssembler datalakeExportAssembler;
    private final TaskExecutor taskExecutor;

    @Value("${custom.platform.token.domain}")
    private String platformDomain;

    public DataLakeBatchPullResultDTO pull(DataLakePullCommand command) {
        List<String> companyCodes = normalizeCompanyCodes(command.getCompanyCodeList());

        List<CompletableFuture<CompanyPullResult>> futures = new ArrayList<>();
        for (String companyCode : companyCodes) {
            futures.add(CompletableFuture.supplyAsync(() -> pullSingleCompany(command, companyCode), taskExecutor));
        }

        DataLakeBatchPullResultDTO result = new DataLakeBatchPullResultDTO();
        for (CompletableFuture<CompanyPullResult> future : futures) {
            CompanyPullResult item = future.join();
            if (item.getError() == null) {
                result.getRecords().addAll(item.getRecords());
            } else {
                result.getErrors().add(item.getError());
            }
        }
        result.setSuccessCount(companyCodes.size() - result.getErrors().size());
        result.setFailCount(result.getErrors().size());
        return result;
    }

    private CompanyPullResult pullSingleCompany(DataLakePullCommand command, String companyCode) {
        try {
            permissionService.checkCompanyAccess(companyCode);

            List<DatalakeDTO> documents = queryFromDataLake(
                    companyCode,
                    command.getFiscalYearPeriodStart(),
                    command.getFiscalYearPeriodEnd()
            );

            Map<FileCategoryEnum, List<DatalakeDTO>> grouped = new EnumMap<>(FileCategoryEnum.class);
            grouped.put(FileCategoryEnum.DL_INCOME, new ArrayList<>());
            grouped.put(FileCategoryEnum.DL_OUTPUT, new ArrayList<>());
            grouped.put(FileCategoryEnum.DL_INPUT, new ArrayList<>());
            grouped.put(FileCategoryEnum.DL_INCOME_TAX, new ArrayList<>());
            grouped.put(FileCategoryEnum.DL_OTHER, new ArrayList<>());
            int droppedCount = 0;

            for (DatalakeDTO dto : documents) {
                FileCategoryEnum category = resolveCategory(dto.getAccount());
                if (category == null) {
                    droppedCount++;
                    continue;
                }
                grouped.get(category).add(dto);
            }
            log.info("datalake split summary, companyCode={}, totalRows={}, income={}, output={}, input={}, incomeTax={}, other={}, dropped={}",
                    companyCode,
                    documents.size(),
                    grouped.get(FileCategoryEnum.DL_INCOME).size(),
                    grouped.get(FileCategoryEnum.DL_OUTPUT).size(),
                    grouped.get(FileCategoryEnum.DL_INPUT).size(),
                    grouped.get(FileCategoryEnum.DL_INCOME_TAX).size(),
                    grouped.get(FileCategoryEnum.DL_OTHER).size(),
                    droppedCount);

            List<FileRecord> records = new ArrayList<>();
            for (Map.Entry<FileCategoryEnum, List<DatalakeDTO>> entry : grouped.entrySet()) {
                List<DatalakeExportRowDTO> exportRows = datalakeExportAssembler.toExportRows(entry.getValue());
                String timestamp = LocalDateTime.now().format(TS_FORMATTER);
                String sheetName = toSheetName(entry.getKey());
                byte[] bytes = toExcelBytes(exportRows, sheetName);
                String blobPath = String.format(
                        "tax-ledger/%s/%s/%s/%s_%s.xlsx",
                        companyCode,
                        command.getYearMonth(),
                        entry.getKey().name(),
                        timestamp,
                        UUID.randomUUID()
                );
                String fileName = toFileTypeName(entry.getKey()) + "-" + timestamp + ".xlsx";
                blobStorageRemote.upload(blobPath, new ByteArrayInputStream(bytes));

                FileRecord record = fileService.saveOrReplace(
                        companyCode,
                        command.getYearMonth(),
                        fileName,
                        entry.getKey(),
                        blobPath,
                        (long) bytes.length
                );
                Object parsedSummary = aggregateParsedResult(entry.getKey(), exportRows);
                String parseResultPath = fileParseOrchestratorService.persistParsedResult(
                        record,
                        parsedSummary,
                        List.of(),
                        "datalake-pull"
                );
                log.info("datalake parse persisted, companyCode={}, yearMonth={}, category={}, rawRows={}, exportRows={}, targetStatus={}",
                        companyCode,
                        command.getYearMonth(),
                        categoryDisplayName(entry.getKey()),
                        entry.getValue().size(),
                        exportRows.size(),
                        "SUCCESS");
                log.info("datalake parse result path, fileId={}, category={}, parseResultBlobPath={}",
                        record.getId(), categoryDisplayName(entry.getKey()), parseResultPath);
                records.add(record);
            }
            return CompanyPullResult.success(records);
        } catch (Exception e) {
            log.error("Datalake pull failed, companyCode={}", companyCode, e);
            return CompanyPullResult.error("companyCode " + companyCode + " pull failed: " + e.getMessage());
        }
    }

    public List<DatalakeDTO> queryFromDataLake(String companyCode,
                                               String fiscalYearPeriodStart,
                                               String fiscalYearPeriodEnd) {
        int offset = 0;
        int limit = 5000;
        String reqUrl = platformDomain + CharSequenceUtil.format(
                DataLakeConstants.FINANCE_ELECTRONICARCHIVES_REQ_PATH_PATTERN,
                companyCode,
                fiscalYearPeriodStart,
                fiscalYearPeriodEnd,
                offset,
                limit
        );
        return platformRemote.fetchFromDataLake(
                DataLakeConstants.FINANCE_ELECTRONICARCHIVES_SVC,
                reqUrl,
                DatalakeDTO::fromPltData
        );
    }

    private byte[] toExcelBytes(List<DatalakeExportRowDTO> exportRows, String sheetName) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            EasyExcelFactory.write(outputStream, DatalakeExportRowDTO.class)
                    .sheet(sheetName)
                    .doWrite(exportRows);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "datalake excel export failed: " + e.getMessage());
        }
    }

    private Object aggregateParsedResult(FileCategoryEnum category, List<DatalakeExportRowDTO> rows) {
        return switch (category) {
            case DL_OUTPUT -> aggregateOutput(rows);
            case DL_OTHER -> aggregateOther(rows);
            case DL_INPUT -> aggregateInput(rows);
            case DL_INCOME, DL_INCOME_TAX -> null;
            default -> null;
        };
    }

    private DlOutputParsedDTO aggregateOutput(List<DatalakeExportRowDTO> rows) {
        DlOutputParsedDTO dto = new DlOutputParsedDTO();
        BigDecimal sum = BigDecimal.ZERO;
        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            sum = sum.add(parseAmount(row.getDocumentAmount()));
        }
        dto.setDocumentAmountSum(sum);
        return dto;
    }

    private DlOtherParsedDTO aggregateOther(List<DatalakeExportRowDTO> rows) {
        DlOtherParsedDTO dto = new DlOtherParsedDTO();
        dto.getDocumentAmountSumByAccount().put(ACCOUNT_INTEREST, BigDecimal.ZERO);
        dto.getDocumentAmountSumByAccount().put(ACCOUNT_OTHER_INCOME, BigDecimal.ZERO);
        dto.getLocalAmountSumByAccount().put(ACCOUNT_INTEREST, BigDecimal.ZERO);
        dto.getLocalAmountSumByAccount().put(ACCOUNT_OTHER_INCOME, BigDecimal.ZERO);

        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            boolean matchedInterest = accountContains(row.getAccount(), ACCOUNT_INTEREST);
            boolean matchedOtherIncome = accountContains(row.getAccount(), ACCOUNT_OTHER_INCOME);
            if (!matchedInterest && !matchedOtherIncome) {
                continue;
            }
            if (matchedInterest) {
                dto.getDocumentAmountSumByAccount().compute(ACCOUNT_INTEREST, (k, v) -> safe(v).add(parseAmount(row.getDocumentAmount())));
                dto.getLocalAmountSumByAccount().compute(ACCOUNT_INTEREST, (k, v) -> safe(v).add(parseAmount(row.getLocalAmount())));
            }
            if (matchedOtherIncome) {
                dto.getDocumentAmountSumByAccount().compute(ACCOUNT_OTHER_INCOME, (k, v) -> safe(v).add(parseAmount(row.getDocumentAmount())));
                dto.getLocalAmountSumByAccount().compute(ACCOUNT_OTHER_INCOME, (k, v) -> safe(v).add(parseAmount(row.getLocalAmount())));
            }
        }
        return dto;
    }

    private DlInputParsedDTO aggregateInput(List<DatalakeExportRowDTO> rows) {
        DlInputParsedDTO dto = new DlInputParsedDTO();
        dto.getLocalAmountSumByAccount().put(ACCOUNT_INPUT_TRANSFER_OUT, BigDecimal.ZERO);

        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (!accountContains(row.getAccount(), ACCOUNT_INPUT_TRANSFER_OUT)) {
                continue;
            }
            dto.getLocalAmountSumByAccount().compute(ACCOUNT_INPUT_TRANSFER_OUT, (k, v) -> safe(v).add(parseAmount(row.getLocalAmount())));
        }
        return dto;
    }

    private BigDecimal parseAmount(String raw) {
        if (CharSequenceUtil.isBlank(raw)) {
            return BigDecimal.ZERO;
        }
        try {
            String normalized = raw.trim().replace(",", "");
            if (normalized.startsWith("(") && normalized.endsWith(")")) {
                normalized = "-" + normalized.substring(1, normalized.length() - 1);
            }
            return new BigDecimal(normalized);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static String normalizeAccount(String account) {
        if (account == null) {
            return "";
        }
        return account.trim();
    }

    private boolean accountContains(String accountText, String targetAccountCode) {
        if (!StringUtils.hasText(targetAccountCode)) {
            return false;
        }
        String normalized = normalizeAccount(accountText);
        return StringUtils.hasText(normalized) && normalized.contains(targetAccountCode);
    }

    private static List<String> normalizeCompanyCodes(List<String> companyCodeList) {
        if (CollectionUtils.isEmpty(companyCodeList)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "companyCodeList must not be empty");
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String item : companyCodeList) {
            if (StringUtils.hasText(item)) {
                set.add(item.trim());
            }
        }
        if (set.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "companyCodeList must not be empty");
        }
        return new ArrayList<>(set);
    }

    private static FileCategoryEnum resolveCategory(String account) {
        String normalized = normalizeAccount(account);
        if (matchesAccountCode(normalized, "2221010200")) {
            return FileCategoryEnum.DL_OUTPUT;
        }
        if (matchesAccountCode(normalized, "2221010100") || matchesAccountCode(normalized, "2221010400")) {
            return FileCategoryEnum.DL_INPUT;
        }
        if (matchesAccountCode(normalized, "2221050000")) {
            return FileCategoryEnum.DL_INCOME_TAX;
        }
        if (normalized.startsWith("6001")) {
            return FileCategoryEnum.DL_INCOME;
        }
        if (matchesAccountCode(normalized, "6603020011") || matchesAccountCode(normalized, "6702000010")) {
            return FileCategoryEnum.DL_OTHER;
        }
        return null;
    }

    private static boolean matchesAccountCode(String normalizedAccount, String accountCode) {
        return StringUtils.hasText(normalizedAccount)
                && StringUtils.hasText(accountCode)
                && normalizedAccount.startsWith(accountCode);
    }

    private static String toSheetName(FileCategoryEnum category) {
        return switch (category) {
            case DL_INCOME -> "income_detail";
            case DL_OUTPUT -> "output_detail";
            case DL_INPUT -> "input_detail";
            case DL_INCOME_TAX -> "income_tax_detail";
            case DL_OTHER -> "other_subject_detail";
            default -> category.name();
        };
    }

    private static String toFileTypeName(FileCategoryEnum category) {
        return switch (category) {
            case DL_INCOME -> "\u6536\u5165\u660e\u7ec6";
            case DL_OUTPUT -> "\u9500\u9879\u660e\u7ec6";
            case DL_INPUT -> "\u8fdb\u9879\u660e\u7ec6";
            case DL_INCOME_TAX -> "\u6240\u5f97\u7a0e\u660e\u7ec6";
            case DL_OTHER -> "\u5176\u4ed6\u79d1\u76ee\u660e\u7ec6";
            default -> category.name();
        };
    }

    private static String categoryDisplayName(FileCategoryEnum category) {
        if (category == null) {
            return "";
        }
        if (CharSequenceUtil.isBlank(category.getDisplayName())) {
            return category.name();
        }
        return category.getDisplayName();
    }

    @lombok.Value(staticConstructor = "of")
    private static class CompanyPullResult {
        List<FileRecord> records;
        String error;

        static CompanyPullResult success(List<FileRecord> records) {
            return CompanyPullResult.of(records, null);
        }

        static CompanyPullResult error(String error) {
            return CompanyPullResult.of(List.of(), error);
        }
    }
}
