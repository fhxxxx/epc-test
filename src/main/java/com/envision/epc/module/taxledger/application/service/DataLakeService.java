package com.envision.epc.module.taxledger.application.service;

import cn.hutool.core.text.CharSequenceUtil;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.facade.platform.PlatformRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.extract.infrastructure.Constant;
import com.envision.epc.module.taxledger.application.command.DataLakePullCommand;
import com.envision.epc.module.taxledger.application.dto.DataLakeBatchPullResultDTO;
import com.envision.epc.module.taxledger.application.dto.DatalakeDTO;
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
import java.nio.charset.StandardCharsets;
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
 * 数据湖拉取与分类服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataLakeService {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final PlatformRemote platformRemote;
    private final BlobStorageRemote blobStorageRemote;
    private final PermissionService permissionService;
    private final FileService fileService;
    private final TaskExecutor taskExecutor;

    @Value("${custom.platform.token.domain}")
    private String platformDomain;

    /**
     * 批量拉取并按规则生成 5 类 DL 文件
     */
    public DataLakeBatchPullResultDTO pull(DataLakePullCommand command) {
        List<String> companyCodes = normalizeCompanyCodes(command.getCompanyCodeList());

        List<CompletableFuture<CompanyPullResult>> futures = new ArrayList<>();
        for (String companyCode : companyCodes) {
            CompletableFuture<CompanyPullResult> future = CompletableFuture.supplyAsync(
                    () -> pullSingleCompany(command, companyCode),
                    taskExecutor
            );
            futures.add(future);
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

            for (DatalakeDTO dto : documents) {
                grouped.get(resolveCategory(dto.getAccount())).add(dto);
            }

            List<FileRecord> records = new ArrayList<>();
            for (Map.Entry<FileCategoryEnum, List<DatalakeDTO>> entry : grouped.entrySet()) {
                byte[] bytes = toCsv(entry.getValue()).getBytes(StandardCharsets.UTF_8);
                String blobPath = String.format("tax-ledger/%s/%s/%s/%s_%s.csv",
                        companyCode,
                        command.getYearMonth(),
                        entry.getKey().name(),
                        LocalDateTime.now().format(TS_FORMATTER),
                        UUID.randomUUID());
                blobStorageRemote.upload(blobPath, new ByteArrayInputStream(bytes));

                FileRecord record = fileService.saveOrReplace(
                        companyCode,
                        command.getYearMonth(),
                        entry.getKey().name() + ".csv",
                        entry.getKey(),
                        blobPath,
                        (long) bytes.length
                );
                records.add(record);
            }
            return CompanyPullResult.success(records);
        } catch (Exception e) {
            log.error("数据湖批量拉取异常，公司代码: {}", companyCode, e);
            return CompanyPullResult.error("公司代码 " + companyCode + " 拉取异常: " + e.getMessage());
        }
    }

    public List<DatalakeDTO> queryFromDataLake(String companyCode,
                                               String fiscalYearPeriodStart,
                                               String fiscalYearPeriodEnd) {
        int offset = 0;
        int limit = 5000;
        String reqUrl = platformDomain + CharSequenceUtil.format(
                Constant.FINANCE_ELECTRONICARCHIVES_REQ_PATH_PATTERN,
                companyCode,
                fiscalYearPeriodStart,
                fiscalYearPeriodEnd,
                offset,
                limit
        );
        return platformRemote.fetchFromDataLake(
                Constant.FINANCE_ELECTRONICARCHIVES_SVC,
                reqUrl,
                DatalakeDTO::fromPltData
        );
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
        if (account == null) {
            return FileCategoryEnum.DL_OTHER;
        }
        if (account.startsWith("222101") || account.startsWith("222102")) {
            return FileCategoryEnum.DL_INPUT;
        }
        if (account.startsWith("6")) {
            return FileCategoryEnum.DL_INCOME;
        }
        if (account.startsWith("2221")) {
            return FileCategoryEnum.DL_OUTPUT;
        }
        if (account.startsWith("25")) {
            return FileCategoryEnum.DL_INCOME_TAX;
        }
        return FileCategoryEnum.DL_OTHER;
    }

    private static String toCsv(List<DatalakeDTO> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("reference,fiscalYearPeriod,companyCode,account,debit,credit,itemText\\n");
        for (DatalakeDTO row : rows) {
            sb.append(safe(row.getReference())).append(",")
                    .append(safe(row.getFiscalYearPeriod())).append(",")
                    .append(safe(row.getCompanyCode())).append(",")
                    .append(safe(row.getAccount())).append(",")
                    .append(row.getDebitAmountInLocalCurrency() == null ? "" : row.getDebitAmountInLocalCurrency()).append(",")
                    .append(row.getCreditAmountInLocalCurrency() == null ? "" : row.getCreditAmountInLocalCurrency()).append(",")
                    .append(safe(row.getItemText())).append("\\n");
        }
        return sb.toString();
    }

    private static String safe(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(",", " ");
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
