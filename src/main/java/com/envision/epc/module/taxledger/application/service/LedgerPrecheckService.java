package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
import com.envision.epc.module.taxledger.application.dto.MonthlyTaxSectionDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PrecheckSnapshotDTO;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativeDeclarationSheetDTO;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativePaymentSheetDTO;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SheetParseService;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileParseStatusEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.domain.LedgerArtifactTypeEnum;
import com.envision.epc.module.taxledger.domain.LedgerRecord;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunArtifactMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * 生成前置校验与快照组装
 */
@Service
@RequiredArgsConstructor
public class LedgerPrecheckService {
    private static final int WAIT_PARSE_MAX_SECONDS = 300;
    private static final String PREVIOUS_LEDGER_REQUIRED_MSG = "未检测到前序台账，请先上传初始台账后再生成。";
    // 临时开关：true 时跳过全部前序台账校验
    private static final boolean SKIP_PREVIOUS_LEDGER_VALIDATION = true;//todo remove

    private final FileRecordMapper fileRecordMapper;
    private final LedgerRecordMapper ledgerRecordMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunArtifactMapper ledgerRunArtifactMapper;
    private final FileParseOrchestratorService fileParseOrchestratorService;
    private final ParsedResultReader parsedResultReader;
    private final SheetParseService sheetParseService;
    private final BlobStorageRemote blobStorageRemote;
    private final TaxLedgerService taxLedgerService;

    public PrecheckSnapshotDTO precheck(String companyCode, String periodMonth) {
        List<String> errors = new ArrayList<>();
        Map<FileCategoryEnum, FileRecord> latestByCategory = latestFileByCategory(companyCode, periodMonth);
        Set<FileCategoryEnum> required = requiredCategories(companyCode);
        List<PrecheckSnapshotDTO.InputItem> inputs = new ArrayList<>();
        Map<FileCategoryEnum, FileRecord> parsedFiles = new HashMap<>();

        for (FileCategoryEnum category : required) {
            FileRecord file = latestByCategory.get(category);
            if (file == null) {
                errors.add("缺少文件: " + categoryDisplayName(category));
                continue;
            }
            FileRecord parsed = ensureParseReady(file, category, errors);
            if (parsed == null) {
                continue;
            }
            parsedFiles.put(category, parsed);
            inputs.add(toInputItem(parsed));
        }

        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, String.join("; ", errors));
        }

        PrecheckSnapshotDTO snapshot = new PrecheckSnapshotDTO();
        snapshot.setCompanyCode(companyCode);
        snapshot.setPeriodMonth(periodMonth);
        snapshot.setRequiredCategories(required.stream().map(Enum::name).sorted().toList());
        snapshot.setInputs(inputs);
        snapshot.setFingerprint(UUID.randomUUID().toString().replace("-", ""));
        snapshot.setGeneratedAt(LocalDateTime.now());
        if (SKIP_PREVIOUS_LEDGER_VALIDATION) {
            snapshot.setPreviousLedgerValidation(buildSkippedPreviousLedgerValidation(periodMonth));
        } else {
            snapshot.setPreviousLedgerValidation(precheckPreviousLedgerPart(companyCode, periodMonth));
        }

        if (isCompany2320Or2355(companyCode)) {
            FileRecord n30File = parsedFiles.get(FileCategoryEnum.PL_APPENDIX_2320);
            FileRecord monthlyFile = parsedFiles.get(FileCategoryEnum.MONTHLY_SETTLEMENT_TAX);
            if (n30File == null || monthlyFile == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "缺少N30校验所需文件");
            }
            PlAppendix23202355DTO uploaded =
                    parsedResultReader.readParsedData(n30File.getParseResultBlobPath(), PlAppendix23202355DTO.class);
            List<MonthlyTaxSectionDTO> monthlyRows =
                    parsedResultReader.readParsedList(monthlyFile.getParseResultBlobPath(), MonthlyTaxSectionDTO.class);
            TaxLedgerService.N30ValidationResult validated = taxLedgerService.validateAndNormalizeN30(uploaded, monthlyRows);
            snapshot.setN30NormalizedData(validated.getData());
            snapshot.setValidationDetails(validated.getValidationDetails());
        }
        return snapshot;
    }

    private PrecheckSnapshotDTO.PreviousLedgerValidation precheckPreviousLedgerPart(String companyCode, String periodMonth) {
        PreviousLedgerContext previous = loadPreviousLedgerContext(companyCode, periodMonth);
        if (previous == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, PREVIOUS_LEDGER_REQUIRED_MSG);
        }

        List<String> errors = new ArrayList<>();
        Map<String, Object> parsedSummary = new LinkedHashMap<>();
        List<String> checkedSheets = new ArrayList<>();

        ProjectCumulativeDeclarationSheetDTO declaration =
                parsePreviousRequired(previous, FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION, companyCode, periodMonth, errors, parsedSummary, checkedSheets);
        ProjectCumulativePaymentSheetDTO payment =
                parsePreviousRequired(previous, FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT, companyCode, periodMonth, errors, parsedSummary, checkedSheets);
        List<UninvoicedMonitorItemDTO> uninvoiced =
                parsePreviousRequired(previous, FileCategoryEnum.UNINVOICED_MONITOR, companyCode, periodMonth, errors, parsedSummary, checkedSheets);

        if (declaration != null) {
            validateProjectCumulativeDeclaration(declaration, errors);
        }
        if (payment != null) {
            validateProjectCumulativePayment(payment, errors);
        }
        if (uninvoiced != null) {
            validateUninvoicedMonitor(uninvoiced, previous.previousPeriodMonth, errors);
        }

        if (isCompany2320Or2355(companyCode)) {
            List<CumulativeTaxSummary23202355ColumnDTO> cumulative =
                    parsePreviousRequired(previous, FileCategoryEnum.CUMULATIVE_TAX_SUMMARY_2320_2355, companyCode, periodMonth, errors, parsedSummary, checkedSheets);
            List<VatTableOneCumulativeOutputItemDTO> vatTableOne =
                    parsePreviousRequired(previous, FileCategoryEnum.VAT_TABLE_ONE_CUMULATIVE_OUTPUT, companyCode, periodMonth, errors, parsedSummary, checkedSheets);
            List<TaxAccountingDifferenceMonitor23202355ItemDTO> taxDiff =
                    parsePreviousRequired(previous, FileCategoryEnum.TAX_ACCOUNTING_DIFFERENCE_MONITOR, companyCode, periodMonth, errors, parsedSummary, checkedSheets);
            List<VatChangeRowDTO> vatChange =
                    parsePreviousRequired(previous, FileCategoryEnum.VAT_CHANGE, companyCode, periodMonth, errors, parsedSummary, checkedSheets);

            if (cumulative != null) {
                validateCumulativeTaxSummary(cumulative, previous.previousPeriodMonth, errors);
            }
            if (vatTableOne != null) {
                validateVatTableOneCumulative(vatTableOne, errors);
            }
            if (taxDiff != null) {
                validateTaxAccountingDifference(taxDiff, errors);
            }
            if (cumulative != null && vatChange != null) {
                validateVatPayableAgainstVatChange(cumulative, vatChange, previous.previousPeriodMonth, errors);
            }
        }

        if (!errors.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, String.join("; ", errors));
        }

        PrecheckSnapshotDTO.PreviousLedgerValidation validation = new PrecheckSnapshotDTO.PreviousLedgerValidation();
        validation.setPreviousPeriodMonth(previous.previousPeriodMonth);
        validation.setPreviousLedgerRunId(previous.runId);
        validation.setPreviousLedgerArtifactPath(previous.artifactPath);
        validation.setCheckedSheets(checkedSheets);
        validation.setIssues(List.of());
        validation.setParsedSummary(parsedSummary);
        return validation;
    }

    private PrecheckSnapshotDTO.PreviousLedgerValidation buildSkippedPreviousLedgerValidation(String periodMonth) {
        PrecheckSnapshotDTO.PreviousLedgerValidation validation = new PrecheckSnapshotDTO.PreviousLedgerValidation();
        String previousPeriod = parseYearMonth(periodMonth).minusMonths(1).toString();
        validation.setPreviousPeriodMonth(previousPeriod);
        validation.setPreviousLedgerRunId(null);
        validation.setPreviousLedgerArtifactPath(null);
        validation.setCheckedSheets(List.of());
        validation.setIssues(List.of("前序台账校验已临时跳过"));
        validation.setParsedSummary(Map.of("skipped", true));
        return validation;
    }

    @SuppressWarnings("unchecked")
    private <T> T parsePreviousRequired(PreviousLedgerContext previous,
                                        FileCategoryEnum category,
                                        String companyCode,
                                        String periodMonth,
                                        List<String> errors,
                                        Map<String, Object> parsedSummary,
                                        List<String> checkedSheets) {
        checkedSheets.add(categoryDisplayName(category));
        ParseContext context = ParseContext.builder()
                .companyCode(companyCode)
                .yearMonth(periodMonth)
                .fileName("previous-ledger")
                .operator("system")
                .traceId(UUID.randomUUID().toString().replace("-", ""))
                .build();
        ParseResult<?> parsed = sheetParseService.parse(new ByteArrayInputStream(previous.ledgerBytes), category, context);
        if (parsed == null) {
            errors.add("前序台账解析失败[" + categoryDisplayName(category) + "]: parse result is null");
            return null;
        }
        if (parsed.hasError()) {
            errors.add("前序台账解析失败[" + categoryDisplayName(category) + "]: " + String.join(", ", parsed.getIssues()));
            return null;
        }
        Object data = parsed.getData();
        if (data == null) {
            errors.add("前序台账解析失败[" + categoryDisplayName(category) + "]: 解析结果为空");
            return null;
        }
        parsedSummary.put(category.name() + ".type", data.getClass().getSimpleName());
        if (data instanceof List<?> list) {
            parsedSummary.put(category.name() + ".size", list.size());
        }
        return (T) data;
    }

    private PreviousLedgerContext loadPreviousLedgerContext(String companyCode, String periodMonth) {
        YearMonth previousYm = parseYearMonth(periodMonth).minusMonths(1);
        String previousPeriod = previousYm.toString();
        List<String> candidates = List.of(previousPeriod.replace("-", ""), previousPeriod);

        // 优先读取文件表中的“最终台账”，允许用户覆盖上传后直接生效
        for (String candidate : candidates) {
            FileRecord finalLedgerFile = fileRecordMapper.selectOne(new LambdaQueryWrapper<FileRecord>()
                    .eq(FileRecord::getIsDeleted, 0)
                    .eq(FileRecord::getCompanyCode, companyCode)
                    .eq(FileRecord::getYearMonth, candidate)
                    .eq(FileRecord::getFileCategory, FileCategoryEnum.FINAL_LEDGER)
                    .orderByDesc(FileRecord::getId)
                    .last("LIMIT 1"));
            if (finalLedgerFile != null && StringUtils.hasText(finalLedgerFile.getBlobPath())) {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try {
                    blobStorageRemote.loadStream(finalLedgerFile.getBlobPath(), output);
                } catch (Exception e) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "读取前序台账失败: " + e.getMessage());
                }
                PreviousLedgerContext context = new PreviousLedgerContext();
                context.previousPeriodMonth = previousPeriod;
                context.runId = null;
                context.artifactPath = finalLedgerFile.getBlobPath();
                context.ledgerBytes = output.toByteArray();
                return context;
            }
        }

        LedgerRecord record = null;
        for (String candidate : candidates) {
            record = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<LedgerRecord>()
                    .eq(LedgerRecord::getIsDeleted, 0)
                    .eq(LedgerRecord::getCompanyCode, companyCode)
                    .eq(LedgerRecord::getYearMonth, candidate)
                    .orderByDesc(LedgerRecord::getId)
                    .last("LIMIT 1"));
            if (record != null) {
                break;
            }
        }
        if (record == null) {
            return null;
        }
        LedgerRun run = ledgerRunMapper.selectOne(new LambdaQueryWrapper<LedgerRun>()
                .eq(LedgerRun::getIsDeleted, 0)
                .eq(LedgerRun::getLedgerId, record.getId())
                .eq(LedgerRun::getStatus, LedgerRunStatusEnum.SUCCESS)
                .orderByDesc(LedgerRun::getRunNo)
                .orderByDesc(LedgerRun::getId)
                .last("LIMIT 1"));
        if (run == null) {
            return null;
        }
        LedgerRunArtifact artifact = ledgerRunArtifactMapper.selectOne(new LambdaQueryWrapper<LedgerRunArtifact>()
                .eq(LedgerRunArtifact::getIsDeleted, 0)
                .eq(LedgerRunArtifact::getRunId, run.getId())
                .eq(LedgerRunArtifact::getArtifactType, LedgerArtifactTypeEnum.FINAL_LEDGER)
                .orderByDesc(LedgerRunArtifact::getId)
                .last("LIMIT 1"));
        if (artifact == null || !StringUtils.hasText(artifact.getBlobPath())) {
            return null;
        }

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            blobStorageRemote.loadStream(artifact.getBlobPath(), output);
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "读取前序台账失败: " + e.getMessage());
        }
        PreviousLedgerContext context = new PreviousLedgerContext();
        context.previousPeriodMonth = previousPeriod;
        context.runId = run.getId();
        context.artifactPath = artifact.getBlobPath();
        context.ledgerBytes = output.toByteArray();
        return context;
    }

    private void validateProjectCumulativeDeclaration(ProjectCumulativeDeclarationSheetDTO dto, List<String> errors) {
        if (dto == null || dto.getRows() == null || dto.getRows().isEmpty()) {
            errors.add("前序台账校验失败[项目累计申报]: 无有效数据行");
            return;
        }
        for (List<ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO> row : dto.getRows()) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            String period = row.stream().map(ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO::getPeriod)
                    .filter(StringUtils::hasText).findFirst().orElse("未知期间");
            BigDecimal total = null;
            BigDecimal sum = BigDecimal.ZERO;
            for (ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO cell : row) {
                if (cell == null || !StringUtils.hasText(cell.getHeaderName())) {
                    continue;
                }
                if (cell.getHeaderName().contains("税款合计")) {
                    total = nvl(cell.getValue());
                    continue;
                }
                sum = sum.add(nvl(cell.getValue()));
            }
            if (total == null) {
                errors.add("前序台账校验失败[项目累计申报]: " + period + " 缺少税款合计列");
                continue;
            }
            if (!decimalEquals(sum, total)) {
                errors.add("前序台账校验失败[项目累计申报]: " + period + " 税款合计不一致");
            }
        }
    }

    private void validateProjectCumulativePayment(ProjectCumulativePaymentSheetDTO dto, List<String> errors) {
        if (dto == null || dto.getRows() == null || dto.getRows().isEmpty()) {
            errors.add("前序台账校验失败[项目累计缴纳]: 无有效数据行");
            return;
        }
        for (ProjectCumulativePaymentSheetDTO.ProjectCumulativePaymentRowDTO row : dto.getRows()) {
            if (row == null || !StringUtils.hasText(row.getPeriod())) {
                continue;
            }
            if (row.getDynamicTaxCells() == null || row.getDynamicTaxCells().isEmpty()) {
                errors.add("前序台账校验失败[项目累计缴纳]: " + row.getPeriod() + " 动态税种列为空");
                continue;
            }
            BigDecimal dynamicSum = row.getDynamicTaxCells().stream()
                    .filter(Objects::nonNull)
                    .map(ProjectCumulativePaymentSheetDTO.ProjectTaxCellDTO::getValue)
                    .map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (!decimalEquals(dynamicSum, nvl(row.getPaidTotal()))) {
                errors.add("前序台账校验失败[项目累计缴纳]: " + row.getPeriod() + " 实缴税金与动态税种合计不一致");
            }
            BigDecimal expectDiff = nvl(row.getPaidTotal()).subtract(nvl(row.getDeclaredTotal()));
            if (!decimalEquals(expectDiff, nvl(row.getDifferenceAmount()))) {
                errors.add("前序台账校验失败[项目累计缴纳]: " + row.getPeriod() + " 差异字段不满足 实缴税金-申请税金");
            }
        }
    }

    private void validateUninvoicedMonitor(List<UninvoicedMonitorItemDTO> rows, String previousPeriodMonth, List<String> errors) {
        if (rows == null || rows.isEmpty()) {
            errors.add("前序台账校验失败[未开票数监控]: 无有效数据行");
            return;
        }
        String expected = toPeriodLabel(previousPeriodMonth);
        boolean exists = rows.stream()
                .filter(Objects::nonNull)
                .map(UninvoicedMonitorItemDTO::getPeriod)
                .filter(StringUtils::hasText)
                .map(this::normalizePeriodLabel)
                .anyMatch(expected::equals);
        if (!exists) {
            errors.add("前序台账校验失败[未开票数监控]: 未找到上期记录 " + expected);
        }
    }

    private void validateCumulativeTaxSummary(List<CumulativeTaxSummary23202355ColumnDTO> cols,
                                              String previousPeriodMonth,
                                              List<String> errors) {
        if (cols == null || cols.isEmpty()) {
            errors.add("前序台账校验失败[累计税金汇总表-2320、2355]: 无期间列数据");
            return;
        }

        List<Integer> ym = cols.stream().map(CumulativeTaxSummary23202355ColumnDTO::getPeriod)
                .map(this::normalizePeriodInt)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        if (ym.isEmpty()) {
            errors.add("前序台账校验失败[累计税金汇总表-2320、2355]: 期间列格式不合法");
            return;
        }
        for (int i = 1; i < ym.size(); i++) {
            if (!isNextMonth(ym.get(i - 1), ym.get(i))) {
                errors.add("前序台账校验失败[累计税金汇总表-2320、2355]: 期间列不连续");
                break;
            }
        }
        Integer expected = normalizePeriodInt(previousPeriodMonth);
        Integer last = ym.get(ym.size() - 1);
        if (expected != null && !expected.equals(last)) {
            errors.add("前序台账校验失败[累计税金汇总表-2320、2355]: 最后一列期间不是上期月份");
        }

        for (CumulativeTaxSummary23202355ColumnDTO col : cols) {
            BigDecimal total = col.getTotalTaxAmount();
            if (total == null) {
                continue;
            }
            BigDecimal sum = nvl(col.getVatAmount())
                    .add(nvl(col.getStampDuty()))
                    .add(nvl(col.getUrbanConstructionTax()))
                    .add(nvl(col.getEducationSurcharge()))
                    .add(nvl(col.getLocalEducationSurcharge()))
                    .add(nvl(col.getPropertyTax()))
                    .add(nvl(col.getUrbanLandUseTax()))
                    .add(nvl(col.getCorporateIncomeTax()))
                    .add(nvl(col.getIndividualIncomeTax()))
                    .add(nvl(col.getDisabledPersonsEmploymentSecurityFund()));
            if (!decimalEquals(sum, total)) {
                errors.add("前序台账校验失败[累计税金汇总表-2320、2355]: " + col.getPeriod() + " 合计列不一致");
            }
        }
    }

    private void validateVatTableOneCumulative(List<VatTableOneCumulativeOutputItemDTO> rows, List<String> errors) {
        if (rows == null || rows.isEmpty()) {
            errors.add("前序台账校验失败[增值税表一 累计销项-2320、2355]: 无有效数据行");
            return;
        }
        for (VatTableOneCumulativeOutputItemDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (!StringUtils.hasText(row.getTaxCategory()) || !row.getTaxCategory().matches(".*\\d+%.*")) {
                errors.add("前序台账校验失败[增值税表一 累计销项-2320、2355]: 涉税类别缺少税率标识");
                break;
            }
            if (row.getTotalAmountExclTax() != null) {
                BigDecimal expectedAmount = nvl(row.getInvoicedAmountExclTax()).add(nvl(row.getUninvoicedAmountExclTax()));
                if (!decimalEquals(expectedAmount, row.getTotalAmountExclTax())) {
                    errors.add("前序台账校验失败[增值税表一 累计销项-2320、2355]: 合计不含税金额不一致");
                    break;
                }
            }
            if (row.getTotalTaxAmount() != null) {
                BigDecimal expectedTax = nvl(row.getInvoicedTaxAmount()).add(nvl(row.getUninvoicedTaxAmount()));
                if (!decimalEquals(expectedTax, row.getTotalTaxAmount())) {
                    errors.add("前序台账校验失败[增值税表一 累计销项-2320、2355]: 合计税额不一致");
                    break;
                }
            }
        }
    }

    private void validateTaxAccountingDifference(List<TaxAccountingDifferenceMonitor23202355ItemDTO> rows, List<String> errors) {
        if (rows == null || rows.isEmpty()) {
            errors.add("前序台账校验失败[账税差异监控-2320、2355]: 无有效数据行");
            return;
        }
        for (TaxAccountingDifferenceMonitor23202355ItemDTO row : rows) {
            if (row == null || row.getCategoryIncomeList() == null || row.getCategoryIncomeList().isEmpty()) {
                errors.add("前序台账校验失败[账税差异监控-2320、2355]: 分类分组为空");
                return;
            }
            BigDecimal bookSum = row.getCategoryIncomeList().stream()
                    .filter(Objects::nonNull)
                    .map(TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem::getBookIncome)
                    .map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal declaredSum = row.getCategoryIncomeList().stream()
                    .filter(Objects::nonNull)
                    .map(TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem::getDeclaredIncome)
                    .map(this::nvl)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            if (row.getTotalBookIncome() != null && !decimalEquals(bookSum, row.getTotalBookIncome())) {
                errors.add("前序台账校验失败[账税差异监控-2320、2355]: 汇总-账面收入不一致");
                return;
            }
            if (row.getTotalVatDeclaredIncome() != null && !decimalEquals(declaredSum, row.getTotalVatDeclaredIncome())) {
                errors.add("前序台账校验失败[账税差异监控-2320、2355]: 汇总-增值税申报收入不一致");
                return;
            }
            BigDecimal diff = nvl(row.getTotalBookIncome()).subtract(nvl(row.getTotalVatDeclaredIncome()));
            if (row.getAccountingTaxDifference() != null && !decimalEquals(diff, row.getAccountingTaxDifference())) {
                errors.add("前序台账校验失败[账税差异监控-2320、2355]: 账税差异列不一致");
                return;
            }
        }
    }

    private void validateVatPayableAgainstVatChange(List<CumulativeTaxSummary23202355ColumnDTO> cumulative,
                                                    List<VatChangeRowDTO> vatChanges,
                                                    String previousPeriod,
                                                    List<String> errors) {
        Integer previous = normalizePeriodInt(previousPeriod);
        CumulativeTaxSummary23202355ColumnDTO current = cumulative.stream()
                .filter(Objects::nonNull)
                .filter(item -> Objects.equals(normalizePeriodInt(item.getPeriod()), previous))
                .findFirst()
                .orElse(null);
        if (current == null) {
            return;
        }
        BigDecimal fromSummary = current.getVatPayableAminusBminusCplusDminusE();
        if (fromSummary == null) {
            return;
        }
        VatChangeRowDTO vatRow = vatChanges.stream()
                .filter(Objects::nonNull)
                .filter(row -> StringUtils.hasText(row.getBaseItem()) && row.getBaseItem().contains("本期应交增值税"))
                .findFirst()
                .orElse(null);
        if (vatRow == null || vatRow.getTotalAmount() == null) {
            errors.add("前序台账校验失败[累计税金汇总表-2320、2355]: 无法在增值税变动表匹配 本期应交增值税");
            return;
        }
        if (!decimalEquals(fromSummary, vatRow.getTotalAmount())) {
            errors.add("前序台账校验失败[累计税金汇总表-2320、2355]: 应纳增值税额 与 增值税变动表不一致");
        }
    }

    private FileRecord ensureParseReady(FileRecord file,
                                        FileCategoryEnum category,
                                        List<String> errors) {
        FileRecord latest = refresh(file.getId());
        if (latest == null || latest.getIsDeleted() == 1) {
            errors.add("文件不存在: " + categoryDisplayName(category));
            return null;
        }
        if (latest.getParseStatus() == FileParseStatusEnum.FAILED) {
            errors.add("文件解析失败: " + categoryDisplayName(category) + " - " + safe(latest.getParseErrorMsg()));
            return null;
        }
        if (latest.getParseStatus() == FileParseStatusEnum.PENDING) {
            fileParseOrchestratorService.parseAsync(latest.getId(), "system");
        }
        if (latest.getParseStatus() == FileParseStatusEnum.PENDING
                || latest.getParseStatus() == FileParseStatusEnum.PARSING) {
            latest = waitParseTerminal(latest.getId());
        }
        if (latest == null || latest.getIsDeleted() == 1) {
            errors.add("文件不存在: " + categoryDisplayName(category));
            return null;
        }
        if (latest.getParseStatus() == FileParseStatusEnum.FAILED) {
            errors.add("文件解析失败: " + categoryDisplayName(category) + " - " + safe(latest.getParseErrorMsg()));
            return null;
        }
        if (latest.getParseStatus() != FileParseStatusEnum.SUCCESS
                || !StringUtils.hasText(latest.getParseResultBlobPath())) {
            errors.add("文件解析未完成: " + categoryDisplayName(category));
            return null;
        }
        return latest;
    }

    private FileRecord waitParseTerminal(Long fileId) {
        int waited = 0;
        while (waited < WAIT_PARSE_MAX_SECONDS) {
            FileRecord latest = refresh(fileId);
            if (latest == null || latest.getIsDeleted() == 1) {
                return latest;
            }
            if (latest.getParseStatus() == FileParseStatusEnum.SUCCESS
                    || latest.getParseStatus() == FileParseStatusEnum.FAILED) {
                return latest;
            }
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "等待文件解析被中断");
            }
            waited++;
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "等待文件解析超时");
    }

    private FileRecord refresh(Long id) {
        return fileRecordMapper.selectById(id);
    }

    private Map<FileCategoryEnum, FileRecord> latestFileByCategory(String companyCode, String periodMonth) {
        List<FileRecord> files = fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, periodMonth)
                .orderByDesc(FileRecord::getUpdateTime)
                .orderByDesc(FileRecord::getId));
        Map<FileCategoryEnum, FileRecord> map = new HashMap<>();
        for (FileRecord file : files) {
            if (file.getFileCategory() == null || map.containsKey(file.getFileCategory())) {
                continue;
            }
            map.put(file.getFileCategory(), file);
        }
        return map;
    }

    private Set<FileCategoryEnum> requiredCategories(String companyCode) {
        Set<FileCategoryEnum> required = EnumSet.of(
                FileCategoryEnum.BS,
                FileCategoryEnum.PL,
                FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE,
                FileCategoryEnum.VAT_OUTPUT,
                FileCategoryEnum.VAT_INPUT_CERT,
                FileCategoryEnum.VAT_CHANGE_APPENDIX,
                FileCategoryEnum.DL_INCOME,
                FileCategoryEnum.DL_OUTPUT,
                FileCategoryEnum.DL_INPUT,
                FileCategoryEnum.DL_INCOME_TAX,
                FileCategoryEnum.DL_OTHER
        );
        if (isCompany2320Or2355(companyCode)) {
            required.add(FileCategoryEnum.PL_APPENDIX_2320);
            required.add(FileCategoryEnum.MONTHLY_SETTLEMENT_TAX);
            required.add(FileCategoryEnum.STAMP_TAX);
            required.add(FileCategoryEnum.CUMULATIVE_PROJECT_TAX);
        } else {
            required.add(FileCategoryEnum.PL_APPENDIX_PROJECT);
            required.add(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER);
        }
        return required;
    }

    private PrecheckSnapshotDTO.InputItem toInputItem(FileRecord record) {
        PrecheckSnapshotDTO.InputItem item = new PrecheckSnapshotDTO.InputItem();
        item.setFileId(record.getId());
        item.setFileName(record.getFileName());
        item.setFileCategory(record.getFileCategory() == null ? null : record.getFileCategory().name());
        item.setParseStatus(record.getParseStatus() == null ? null : record.getParseStatus().name());
        item.setParseResultBlobPath(record.getParseResultBlobPath());
        item.setFileSize(record.getFileSize());
        return item;
    }

    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }

    private YearMonth parseYearMonth(String yearMonth) {
        String normalized = safe(yearMonth).trim();
        if (normalized.matches("^\\d{6}$")) {
            return YearMonth.parse(normalized.substring(0, 4) + "-" + normalized.substring(4, 6));
        }
        if (normalized.matches("^\\d{4}-\\d{2}$")) {
            return YearMonth.parse(normalized);
        }
        throw new BizException(ErrorCode.BAD_REQUEST, "invalid yearMonth format: " + normalized);
    }

    private String toPeriodLabel(String yearMonth) {
        YearMonth ym = parseYearMonth(yearMonth);
        return ym.getYear() + "-" + ym.getMonthValue();
    }

    private String normalizePeriodLabel(String period) {
        if (!StringUtils.hasText(period)) {
            return "";
        }
        String text = period.trim();
        if (text.matches("^\\d{6}$")) {
            return Integer.parseInt(text.substring(0, 4)) + "-" + Integer.parseInt(text.substring(4, 6));
        }
        if (text.matches("^\\d{4}-\\d{1,2}$")) {
            String[] parts = text.split("-");
            return Integer.parseInt(parts[0]) + "-" + Integer.parseInt(parts[1]);
        }
        return text;
    }

    private Integer normalizePeriodInt(String period) {
        if (!StringUtils.hasText(period)) {
            return null;
        }
        String text = period.trim();
        if (text.matches("^\\d{6}$")) {
            return Integer.parseInt(text);
        }
        if (text.matches("^\\d{4}-\\d{2}$")) {
            return Integer.parseInt(text.replace("-", ""));
        }
        return null;
    }

    private boolean isNextMonth(int prev, int next) {
        int prevYear = prev / 100;
        int prevMonth = prev % 100;
        int nextYear = next / 100;
        int nextMonth = next % 100;
        if (prevMonth == 12) {
            return nextYear == prevYear + 1 && nextMonth == 1;
        }
        return nextYear == prevYear && nextMonth == prevMonth + 1;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean decimalEquals(BigDecimal a, BigDecimal b) {
        return nvl(a).subtract(nvl(b)).abs().compareTo(new BigDecimal("0.01")) <= 0;
    }

    private String categoryDisplayName(FileCategoryEnum category) {
        if (category == null) {
            return "";
        }
        if (!StringUtils.hasText(category.getDisplayName())) {
            return category.name();
        }
        return category.getDisplayName();
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "unknown error";
    }

    private static class PreviousLedgerContext {
        private String previousPeriodMonth;
        private Long runId;
        private String artifactPath;
        private byte[] ledgerBytes;
    }
}
