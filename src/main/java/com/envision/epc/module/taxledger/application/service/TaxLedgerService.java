package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.command.CreateLedgerRunCommand;
import com.envision.epc.module.taxledger.application.dto.LedgerRunDetailDTO;
import com.envision.epc.module.taxledger.application.dto.MonthlySettlementTaxParsedDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerConfigSnapshot;
import com.envision.epc.module.taxledger.application.ledger.LedgerParsedDataGateway;
import com.envision.epc.module.taxledger.application.ledger.LedgerParsedDataGatewayFactory;
import com.envision.epc.module.taxledger.application.ledger.ParsedResultTypeCatalog;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.SummaryQuarterSnapshot;
import com.envision.epc.module.taxledger.application.ledger.LedgerWorkbookData;
import com.envision.epc.module.taxledger.application.ledger.LedgerWorkbookDataAssembler;
import com.envision.epc.module.taxledger.domain.CompanyCodeConfig;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.domain.LedgerArtifactTypeEnum;
import com.envision.epc.module.taxledger.domain.LedgerGenerateStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRecord;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.LedgerRunModeEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunTriggerEnum;
import com.envision.epc.module.taxledger.domain.ProjectConfig;
import com.envision.epc.module.taxledger.domain.TaxCategoryConfig;
import com.envision.epc.module.taxledger.domain.VatBasicItemConfig;
import com.envision.epc.module.taxledger.infrastructure.CompanyCodeConfigMapper;
import com.envision.epc.module.taxledger.excel.LedgerBuildOutput;
import com.envision.epc.module.taxledger.excel.TaxLedgerExcelService;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunArtifactMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunMapper;
import com.envision.epc.module.taxledger.infrastructure.ProjectConfigMapper;
import com.envision.epc.module.taxledger.infrastructure.TaxCategoryConfigMapper;
import com.envision.epc.module.taxledger.infrastructure.VatBasicItemConfigMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 台账运行核心服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxLedgerService {
    private static final Pattern TAX_RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]");
    private static final String N30_FILE_LABEL = "PL附表-2320、2355";
    private final LedgerRecordMapper ledgerRecordMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunArtifactMapper artifactMapper;
    private final FileRecordMapper fileRecordMapper;
    private final CompanyCodeConfigMapper companyCodeConfigMapper;
    private final TaxCategoryConfigMapper taxCategoryConfigMapper;
    private final ProjectConfigMapper projectConfigMapper;
    private final VatBasicItemConfigMapper vatBasicItemConfigMapper;
    private final BlobStorageRemote blobStorageRemote;
    private final LedgerParsedDataGatewayFactory parsedDataGatewayFactory;
    private final LedgerWorkbookDataAssembler workbookDataAssembler;
    private final TaxLedgerExcelService excelService;
    private final SummaryQuarterDataPreparer summaryQuarterDataPreparer;
    private final PermissionService permissionService;
    private final TaskExecutor taskExecutor;
    private final ObjectMapper objectMapper;

    /**
     * 创建新运行
     */
    @Transactional(rollbackFor = Exception.class)
    public LedgerRunDetailDTO createRun(CreateLedgerRunCommand command) {
        permissionService.checkCompanyAccess(command.getCompanyCode());

        LedgerRecord ledger = getOrCreateLedgerRecord(command.getCompanyCode(), command.getYearMonth());
        invalidateOldRuns(ledger.getId());

        LedgerRun run = new LedgerRun();
        run.setLedgerId(ledger.getId());
        run.setRunNo(nextRunNo(ledger.getId()));
        run.setTriggerType(LedgerRunTriggerEnum.MANUAL);
        run.setModeSnapshot(LedgerRunModeEnum.AUTO);
        run.setStatus(LedgerRunStatusEnum.RUNNING);
        run.setCurrentBatch(1);
        run.setInputFingerprint(UUID.randomUUID().toString().replace("-", ""));
        run.setStartedAt(LocalDateTime.now());
        run.setIsDeleted(0);
        ledgerRunMapper.insert(run);

        ledger.setGenerateStatus(LedgerGenerateStatusEnum.PENDING);
        ledger.setStatusMsg("Run started");
        ledgerRecordMapper.updateById(ledger);

        dispatchRunAfterCommit(run.getId());
        return getRunDetail(run.getId());
    }

    /**
     * 查询运行详情
     */
    public LedgerRunDetailDTO getRunDetail(Long runId) {
        LedgerRun run = ledgerRunMapper.selectById(runId);
        if (run == null || run.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Run not found");
        }

        LedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
        permissionService.checkCompanyAccess(ledger.getCompanyCode());

        List<LedgerRunArtifact> artifacts = artifactMapper.selectList(new LambdaQueryWrapper<LedgerRunArtifact>()
                .eq(LedgerRunArtifact::getIsDeleted, 0)
                .eq(LedgerRunArtifact::getRunId, runId)
                .orderByAsc(LedgerRunArtifact::getBatchNo));
        return LedgerRunDetailDTO.of(run, List.of(), artifacts, List.of(), null, List.of());
    }


    /**
     * 查询运行历史
     */
    public List<LedgerRun> listRuns(String companyCode, String yearMonth) {
        permissionService.checkCompanyAccess(companyCode);
        LedgerRecord ledger = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<LedgerRecord>()
                .eq(LedgerRecord::getIsDeleted, 0)
                .eq(LedgerRecord::getCompanyCode, companyCode)
                .eq(LedgerRecord::getYearMonth, yearMonth));
        if (ledger == null) {
            return Collections.emptyList();
        }

        return ledgerRunMapper.selectList(new LambdaQueryWrapper<LedgerRun>()
                .eq(LedgerRun::getIsDeleted, 0)
                .eq(LedgerRun::getLedgerId, ledger.getId())
                .orderByDesc(LedgerRun::getRunNo));
    }

    /**
     * 下载最终台账
     */
    public void downloadFinalLedger(String companyCode, String yearMonth, HttpServletResponse response) throws IOException {
        permissionService.checkCompanyAccess(companyCode);
        LedgerRecord ledger = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<LedgerRecord>()
                .eq(LedgerRecord::getIsDeleted, 0)
                .eq(LedgerRecord::getCompanyCode, companyCode)
                .eq(LedgerRecord::getYearMonth, yearMonth));
        if (ledger == null || ledger.getBlobPath() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Final ledger is not available");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode(ledger.getLedgerName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        blobStorageRemote.loadStream(ledger.getBlobPath(), response.getOutputStream());
    }

    private void dispatchRun(Long runId) {
        taskExecutor.execute(() -> processRun(runId));
    }

    private void dispatchRunAfterCommit(Long runId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchRun(runId);
                }
            });
            return;
        }
        dispatchRun(runId);
    }

    private void processRun(Long runId) {
        try {
            LedgerRun run = ledgerRunMapper.selectById(runId);
            if (run == null || run.getIsDeleted() == 1 || run.getStatus() != LedgerRunStatusEnum.RUNNING) {
                return;
            }
            LedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
            finalizeRunSuccess(run, ledger);
        } catch (Exception e) {
            markRunFailed(runId, e.getMessage());
            log.error("run process failed, runId={}", runId, e);
        }
    }

    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }

    private Map<FileCategoryEnum, FileRecord> latestFileByCategory(List<FileRecord> files) {
        Map<FileCategoryEnum, FileRecord> map = new HashMap<>();
        for (FileRecord file : files) {
            if (file.getFileCategory() == null) {
                continue;
            }
            FileRecord existing = map.get(file.getFileCategory());
            if (existing == null || (file.getId() != null && existing.getId() != null && file.getId() > existing.getId())) {
                map.put(file.getFileCategory(), file);
            }
        }
        return map;
    }

    public N30ValidationResult validateAndNormalizeN30(PlAppendix23202355DTO uploaded, MonthlySettlementTaxParsedDTO monthly) {
        List<PlAppendix23202355DTO.InvoicingSplitItem> section1 =
                uploaded.getInvoicingSplitList() == null ? new ArrayList<>() : new ArrayList<>(uploaded.getInvoicingSplitList());
        List<PlAppendix23202355DTO.DeclarationSplitItem> section2 =
                uploaded.getDeclarationSplitList() == null ? new ArrayList<>() : new ArrayList<>(uploaded.getDeclarationSplitList());

        removeBlankNormalRows(section1, section2);
        removeTotalRows(section1, section2);
        removeBlankPupiaoRows(section1, section2);

        Map<String, PlAppendix23202355DTO.InvoicingSplitItem> sec1Map = new LinkedHashMap<>();
        Map<String, PlAppendix23202355DTO.DeclarationSplitItem> sec2Map = new LinkedHashMap<>();
        for (PlAppendix23202355DTO.InvoicingSplitItem row : section1) {
            String key = splitBasisKey(row.getSplitBasis(), "section1");
            if (sec1Map.putIfAbsent(key, row) != null) {
                throw new BizException(ErrorCode.BAD_REQUEST, N30_FILE_LABEL + "：Section1拆分依据重复: " + row.getSplitBasis());
            }
        }
        for (PlAppendix23202355DTO.DeclarationSplitItem row : section2) {
            String key = splitBasisKey(row.getSplitBasis(), "section2");
            if (sec2Map.putIfAbsent(key, row) != null) {
                throw new BizException(ErrorCode.BAD_REQUEST, N30_FILE_LABEL + "：Section2拆分依据重复: " + row.getSplitBasis());
            }
        }

        if (!sec1Map.keySet().equals(sec2Map.keySet())) {
            Set<String> only1 = new HashSet<>(sec1Map.keySet());
            only1.removeAll(sec2Map.keySet());
            Set<String> only2 = new HashSet<>(sec2Map.keySet());
            only2.removeAll(sec1Map.keySet());
            throw new BizException(ErrorCode.BAD_REQUEST, N30_FILE_LABEL + "：上下Section拆分依据不一致, section1Only="
                    + String.join(",", only1) + ", section2Only=" + String.join(",", only2));
        }

        for (String key : sec2Map.keySet()) {
            if (key.startsWith("专票-")) {
                String rate = key.substring("专票-".length());
                sec2Map.get(key).setDeclaredAmount(sumMonthlyByRate(monthly, rate, MonthlyField.INCOME));
                sec2Map.get(key).setDeclaredTaxAmount(sumMonthlyByRate(monthly, rate, MonthlyField.OUTPUT_TAX));
            }
        }

        for (String key : sec1Map.keySet()) {
            PlAppendix23202355DTO.InvoicingSplitItem s1 = sec1Map.get(key);
            PlAppendix23202355DTO.DeclarationSplitItem s2 = sec2Map.get(key);
            if (key.startsWith("专票-")) {
                String rate = key.substring("专票-".length());
                s1.setInvoicedIncome(sumMonthlyByRate(monthly, rate, MonthlyField.INVOICED_INCOME));
                s1.setInvoicedOutputTax(sumMonthlyByRate(monthly, rate, MonthlyField.INVOICED_TAX_AMOUNT));
                s1.setUninvoicedIncome(subtract(s2.getDeclaredAmount(), s1.getInvoicedIncome()));
                s1.setOutputTax(subtract(s2.getDeclaredTaxAmount(), s1.getInvoicedOutputTax()));
            }
        }

        PlAppendix23202355DTO normalized = new PlAppendix23202355DTO();
        normalized.setInvoicingSplitList(new ArrayList<>(sec1Map.values()));
        normalized.setDeclarationSplitList(new ArrayList<>(sec2Map.values()));

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("errors", List.of());
        details.put("warnings", List.of());
        details.put("matchedKeys", new ArrayList<>(sec1Map.keySet()));
        details.put("section1Count", sec1Map.size());
        details.put("section2Count", sec2Map.size());
        return new N30ValidationResult(normalized, details);
    }

    private void removeBlankNormalRows(List<PlAppendix23202355DTO.InvoicingSplitItem> section1,
                                       List<PlAppendix23202355DTO.DeclarationSplitItem> section2) {
        section1.removeIf(row -> normalizeText(row.getSplitBasis()) == null);
        section2.removeIf(row -> normalizeText(row.getSplitBasis()) == null);
    }

    private void removeTotalRows(List<PlAppendix23202355DTO.InvoicingSplitItem> section1,
                                 List<PlAppendix23202355DTO.DeclarationSplitItem> section2) {
        section1.removeIf(row -> isTotalRow(row.getSplitBasis()));
        section2.removeIf(row -> isTotalRow(row.getSplitBasis()));
    }

    private void removeBlankPupiaoRows(List<PlAppendix23202355DTO.InvoicingSplitItem> section1,
                                       List<PlAppendix23202355DTO.DeclarationSplitItem> section2) {
        section1.removeIf(row -> isPupiao(row.getSplitBasis()) && allNull(
                row.getUninvoicedIncome(), row.getOutputTax(), row.getInvoicedIncome(), row.getInvoicedOutputTax()));
        section2.removeIf(row -> isPupiao(row.getSplitBasis()) && allNull(
                row.getDeclaredAmount(), row.getDeclaredTaxAmount()));
    }

    private boolean allNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return false;
            }
        }
        return true;
    }

    private String splitBasisKey(String splitBasis, String sectionName) {
        String text = normalizeText(splitBasis);
        if (text == null || text.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, N30_FILE_LABEL + "：" + sectionName + "拆分依据不能为空");
        }
        String rate = extractTaxRate(text);
        if (rate == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, N30_FILE_LABEL + "：" + sectionName + "拆分依据无法提取税率: " + text);
        }
        return (isPupiao(text) ? "普票-" : "专票-") + rate;
    }

    private boolean isTotalRow(String splitBasis) {
        String text = normalizeText(splitBasis);
        return text != null && text.contains("合计");
    }

    private boolean isPupiao(String splitBasis) {
        String text = normalizeText(splitBasis);
        return text != null && text.contains("普票");
    }

    private String extractTaxRate(String text) {
        if (text == null) {
            return null;
        }
        Matcher matcher = TAX_RATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "%";
    }

    private BigDecimal sumMonthlyByRate(MonthlySettlementTaxParsedDTO monthly, String rate, MonthlyField field) {
        if (monthly == null || monthly.getAggregateByRate() == null || rate == null) {
            return BigDecimal.ZERO;
        }
        MonthlySettlementTaxParsedDTO.RateAggregate aggregate = monthly.getAggregateByRate().get(rate);
        if (aggregate == null) {
            return BigDecimal.ZERO;
        }
        return switch (field) {
            case INCOME -> nvl(aggregate.getIncomeSum());
            case OUTPUT_TAX -> nvl(aggregate.getOutputTaxSum());
            case INVOICED_INCOME -> nvl(aggregate.getInvoicedIncomeSum());
            case INVOICED_TAX_AMOUNT -> nvl(aggregate.getInvoicedTaxAmountSum());
        };
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private boolean matchesCompanyCode(String configCompanyCode, String companyCode) {
        String target = normalizeText(companyCode);
        if (target == null || target.isEmpty()) {
            return false;
        }
        String cfg = normalizeText(configCompanyCode);
        if (cfg == null || cfg.isEmpty()) {
            return true;
        }
        String normalized = cfg.replace('，', ',');
        String[] parts = normalized.split(",");
        for (String part : parts) {
            if (target.equals(normalizeText(part))) {
                return true;
            }
        }
        return false;
    }

    private BigDecimal subtract(BigDecimal a, BigDecimal b) {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        BigDecimal right = b == null ? BigDecimal.ZERO : b;
        return left.subtract(right);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private void finalizeRunSuccess(LedgerRun run, LedgerRecord ledger) throws Exception {
        List<FileRecord> files = loadFiles(ledger.getCompanyCode(), ledger.getYearMonth());
        LedgerBuildContext buildContext = buildLedgerContext(run, ledger, files);

        LedgerWorkbookData workbookData = workbookDataAssembler.buildAll(buildContext);
        LedgerRenderContext renderContext = LedgerRenderContext.builder()
                .companyCode(ledger.getCompanyCode())
                .yearMonth(ledger.getYearMonth())
                .templateLoader("classpath")
                .stylePolicy("DEFAULT")
                .formulaPolicy("DYNAMIC")
                .build();
        LedgerBuildOutput output = excelService.buildLedger(workbookData, renderContext);
        if (output.getBuildReport() != null && buildContext.getPreloadSummary() != null) {
            output.getBuildReport().put("preloadSummary", buildContext.getPreloadSummary());
        }
        byte[] finalLedger = output.getWorkbookBytes();
        String blobPath = String.format("tax-ledger/%s/%s/final/%s",
                ledger.getCompanyCode(), ledger.getYearMonth(), ledger.getLedgerName());
        blobStorageRemote.upload(blobPath, new ByteArrayInputStream(finalLedger));
        int finalBatchNo = 1;
        saveArtifact(run.getId(), finalBatchNo, LedgerArtifactTypeEnum.FINAL_LEDGER, ledger.getLedgerName(), blobPath,
                (long) finalLedger.length, sha256(finalLedger));
        writeLedgerBuildReportArtifact(run.getId(), finalBatchNo, ledger, output.getBuildReport());

        run.setStatus(LedgerRunStatusEnum.SUCCESS);
        run.setEndedAt(LocalDateTime.now());
        ledgerRunMapper.updateById(run);

        ledger.setGenerateStatus(LedgerGenerateStatusEnum.SUCCESS);
        ledger.setGeneratedAt(LocalDateTime.now());
        ledger.setStatusMsg("Success");
        ledger.setBlobPath(blobPath);
        ledgerRecordMapper.updateById(ledger);
    }

    private LedgerBuildContext buildLedgerContext(LedgerRun run,
                                                  LedgerRecord ledger,
                                                  List<FileRecord> files) {
        if (ledger == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装 LedgerBuildContext 失败: ledger 为空");
        }
        if (run == null || run.getId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装 LedgerBuildContext 失败: runId 为空");
        }
        if (ledger.getCompanyCode() == null || ledger.getCompanyCode().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装 LedgerBuildContext 失败: companyCode 为空");
        }
        if (ledger.getYearMonth() == null || ledger.getYearMonth().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装 LedgerBuildContext 失败: yearMonth 为空");
        }
        List<FileRecord> safeFiles = files == null ? List.of() : files;
        LedgerParsedDataGateway parsedDataGateway = parsedDataGatewayFactory.create(safeFiles);
        if (parsedDataGateway == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装 LedgerBuildContext 失败: parsedDataGateway 为空");
        }
        PreloadOutcome preload = preloadParsedData(safeFiles, parsedDataGateway);
        LedgerConfigSnapshot configSnapshot = loadConfigSnapshot(ledger.getCompanyCode());
        Map<String, Object> preloadSummary = new LinkedHashMap<>(preload.summary());
        preloadSummary.put("configSnapshot", configSnapshot.summary());
        SummaryQuarterSnapshot summaryQuarterSnapshot = summaryQuarterDataPreparer.prepare(ledger.getCompanyCode(), ledger.getYearMonth());
        preloadSummary.put("summaryQuarterMonths", summaryQuarterSnapshot.getQuarterMonths());
        preloadSummary.put("summaryMissingPlMonths", summaryQuarterSnapshot.getMissingMonths());
        preloadSummary.put("summaryQuarterWarnings", summaryQuarterSnapshot.getWarnings());

        LedgerBuildContext context = LedgerBuildContext.builder()
                .companyCode(ledger.getCompanyCode())
                .yearMonth(ledger.getYearMonth())
                .files(safeFiles)
                .traceId(String.valueOf(run.getId()))
                .operator("system")
                .parsedDataGateway(parsedDataGateway)
                .configSnapshot(configSnapshot)
                .preloadedParsedData(preload.preloadedParsedData())
                .preloadSummary(preloadSummary)
                .summaryQuarterSnapshot(summaryQuarterSnapshot)
                .builtSheetDataMap(new LinkedHashMap<>())
                .build();
        log.info("ledger context built: runId={}, companyCode={}, yearMonth={}, filesCount={}, preloadTotal={}, preloadSuccess={}, preloadFailed={}, preloadSkipped={}, failedCategories={}",
                run.getId(),
                context.getCompanyCode(),
                context.getYearMonth(),
                safeFiles.size(),
                preload.summary().getOrDefault("preloadTotal", 0),
                preload.summary().getOrDefault("preloadSuccess", 0),
                preload.summary().getOrDefault("preloadFailed", 0),
                preload.summary().getOrDefault("preloadSkipped", 0),
                preload.summary().getOrDefault("failedCategories", List.of()));
        return context;
    }

    private LedgerConfigSnapshot loadConfigSnapshot(String companyCode) {
        CompanyCodeConfig currentCompany = companyCodeConfigMapper.selectOne(new LambdaQueryWrapper<CompanyCodeConfig>()
                .eq(CompanyCodeConfig::getIsDeleted, 0)
                .eq(CompanyCodeConfig::getCompanyCode, companyCode)
                .last("LIMIT 1"));
        List<CompanyCodeConfig> companyCodeConfigs = companyCodeConfigMapper.selectList(new LambdaQueryWrapper<CompanyCodeConfig>()
                .eq(CompanyCodeConfig::getIsDeleted, 0)
                .orderByAsc(CompanyCodeConfig::getCompanyCode));
        List<TaxCategoryConfig> taxCategoryConfigs = taxCategoryConfigMapper.selectList(new LambdaQueryWrapper<TaxCategoryConfig>()
                .eq(TaxCategoryConfig::getIsDeleted, 0)
                .and(w -> w.isNull(TaxCategoryConfig::getCompanyCode)
                        .or().eq(TaxCategoryConfig::getCompanyCode, companyCode))
                .orderByAsc(TaxCategoryConfig::getSeqNo));
        List<ProjectConfig> projectConfigs = projectConfigMapper.selectList(new LambdaQueryWrapper<ProjectConfig>()
                .eq(ProjectConfig::getIsDeleted, 0)
                .and(w -> w.isNull(ProjectConfig::getCompanyCode)
                        .or().eq(ProjectConfig::getCompanyCode, "")
                        .or().eq(ProjectConfig::getCompanyCode, companyCode)));
        List<VatBasicItemConfig> vatBasicItemConfigs = vatBasicItemConfigMapper.selectList(new LambdaQueryWrapper<VatBasicItemConfig>()
                .eq(VatBasicItemConfig::getIsDeleted, 0)
                .orderByAsc(VatBasicItemConfig::getItemSeq));
        vatBasicItemConfigs = vatBasicItemConfigs.stream()
                .filter(cfg -> matchesCompanyCode(cfg == null ? null : cfg.getCompanyCode(), companyCode))
                .toList();

        return LedgerConfigSnapshot.builder()
                .companyCode(companyCode)
                .currentCompany(currentCompany)
                .companyCodeConfigs(companyCodeConfigs)
                .taxCategoryConfigs(taxCategoryConfigs)
                .projectConfigs(projectConfigs)
                .vatBasicItemConfigs(vatBasicItemConfigs)
                .build();
    }

    private PreloadOutcome preloadParsedData(List<FileRecord> files, LedgerParsedDataGateway parsedDataGateway) {
        Map<FileCategoryEnum, Object> preloaded = new HashMap<>();
        Map<String, Object> summary = new LinkedHashMap<>();
        List<String> failedCategories = new ArrayList<>();
        List<String> skippedCategories = new ArrayList<>();

        Map<FileCategoryEnum, FileRecord> latestByCategory = latestFileByCategory(files);
        int total = 0;
        int success = 0;
        int failed = 0;
        int skipped = 0;

        for (Map.Entry<FileCategoryEnum, FileRecord> entry : latestByCategory.entrySet()) {
            FileCategoryEnum category = entry.getKey();
            FileRecord file = entry.getValue();
            if (!ParsedResultTypeCatalog.supports(category)) {
                continue;
            }
            if (file == null || file.getParseResultBlobPath() == null || file.getParseResultBlobPath().isBlank()) {
                skipped++;
                skippedCategories.add(category.name());
                continue;
            }
            total++;
            ParsedResultTypeCatalog.Entry type = ParsedResultTypeCatalog.get(category);
            try {
                Object value = loadByCatalog(parsedDataGateway, category, type);
                preloaded.put(category, value);
                success++;
            } catch (Exception ex) {
                failed++;
                failedCategories.add(category.name());
                log.warn("preload parsed data failed, category={}, fileId={}, reason={}",
                        category.name(), file.getId(), ex.getMessage());
            }
        }

        summary.put("preloadTotal", total);
        summary.put("preloadSuccess", success);
        summary.put("preloadFailed", failed);
        summary.put("preloadSkipped", skipped);
        summary.put("failedCategories", failedCategories);
        summary.put("skippedCategories", skippedCategories);
        return new PreloadOutcome(preloaded, summary);
    }

    @SuppressWarnings("unchecked")
    private Object loadByCatalog(LedgerParsedDataGateway gateway,
                                 FileCategoryEnum category,
                                 ParsedResultTypeCatalog.Entry type) {
        if (type == null) {
            return null;
        }
        if (type.shape() == ParsedResultTypeCatalog.Shape.LIST) {
            return gateway.readParsedList(category, (Class<Object>) type.valueType());
        }
        return gateway.readParsedObject(category, (Class<Object>) type.valueType());
    }

    private record PreloadOutcome(Map<FileCategoryEnum, Object> preloadedParsedData,
                                  Map<String, Object> summary) {
    }

    private void writeLedgerBuildReportArtifact(Long runId,
                                                Integer batchNo,
                                                LedgerRecord ledger,
                                                Map<String, Object> report) {
        if (report == null || report.isEmpty()) {
            return;
        }
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(report);
            String path = String.format("tax-ledger/%s/%s/runs/%d/ledger-build-report.json",
                    ledger.getCompanyCode(), ledger.getYearMonth(), runId);
            blobStorageRemote.upload(path, new ByteArrayInputStream(bytes));
            saveArtifact(runId, batchNo, LedgerArtifactTypeEnum.LEDGER_BUILD_REPORT,
                    "ledger-build-report.json", path, (long) bytes.length, sha256(bytes));
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "write ledger build report failed: " + e.getMessage());
        }
    }

    private List<FileRecord> loadFiles(String companyCode, String yearMonth) {
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, yearMonth));
    }

    private void markRunFailed(Long runId, String msg) {
        LedgerRun run = ledgerRunMapper.selectById(runId);
        if (run == null || run.getIsDeleted() == 1) {
            return;
        }
        run.setStatus(LedgerRunStatusEnum.FAILED);
        run.setErrorCode("RUN_FAILED");
        run.setErrorMsg(msg);
        run.setEndedAt(LocalDateTime.now());
        ledgerRunMapper.updateById(run);

        LedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
        if (ledger != null) {
            ledger.setGenerateStatus(LedgerGenerateStatusEnum.FAILED);
            ledger.setStatusMsg(msg);
            ledgerRecordMapper.updateById(ledger);
        }
    }

    private void saveArtifact(Long runId,
                              Integer batchNo,
                              LedgerArtifactTypeEnum type,
                              String fileName,
                              String path,
                              Long fileSize,
                              String checksum) {
        LedgerRunArtifact artifact = new LedgerRunArtifact();
        artifact.setRunId(runId);
        artifact.setBatchNo(batchNo);
        artifact.setArtifactType(type);
        artifact.setFileName(fileName);
        artifact.setBlobPath(path);
        artifact.setFileSize(fileSize);
        artifact.setChecksum(checksum);
        artifact.setIsLatest(1);
        artifact.setIsDeleted(0);
        artifactMapper.insert(artifact);
    }

    private static String sha256(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "N/A";
        }
    }

    private LedgerRecord getOrCreateLedgerRecord(String companyCode, String yearMonth) {
        LedgerRecord ledger = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<LedgerRecord>()
                .eq(LedgerRecord::getIsDeleted, 0)
                .eq(LedgerRecord::getCompanyCode, companyCode)
                .eq(LedgerRecord::getYearMonth, yearMonth));
        if (ledger != null) {
            return ledger;
        }

        ledger = new LedgerRecord();
        ledger.setCompanyCode(companyCode);
        ledger.setYearMonth(yearMonth);
        ledger.setLedgerName(companyCode + "-" + yearMonth + "-tax-ledger.xlsx");
        ledger.setBlobPath("");
        ledger.setGenerateStatus(LedgerGenerateStatusEnum.PENDING);
        ledger.setStatusMsg("Not generated");
        ledger.setGenerateUser(permissionService.currentUserCode());
        ledger.setIsDeleted(0);
        ledgerRecordMapper.insert(ledger);
        return ledger;
    }

    private void invalidateOldRuns(Long ledgerId) {
        List<LedgerRun> oldRuns = ledgerRunMapper.selectList(new LambdaQueryWrapper<LedgerRun>()
                .eq(LedgerRun::getIsDeleted, 0)
                .eq(LedgerRun::getLedgerId, ledgerId)
                .in(LedgerRun::getStatus,
                        LedgerRunStatusEnum.PENDING,
                        LedgerRunStatusEnum.RUNNING,
                        LedgerRunStatusEnum.PAUSED));

        oldRuns.forEach(item -> {
            item.setStatus(LedgerRunStatusEnum.INVALIDATED);
            item.setEndedAt(LocalDateTime.now());
            ledgerRunMapper.updateById(item);
        });
    }

    private int nextRunNo(Long ledgerId) {
        LedgerRun latest = ledgerRunMapper.selectOne(new LambdaQueryWrapper<LedgerRun>()
                .eq(LedgerRun::getIsDeleted, 0)
                .eq(LedgerRun::getLedgerId, ledgerId)
                .orderByDesc(LedgerRun::getRunNo)
                .last("LIMIT 1"));
        if (latest == null) {
            return 1;
        }
        return latest.getRunNo() + 1;
    }

    private enum MonthlyField {
        INCOME,
        OUTPUT_TAX,
        INVOICED_INCOME,
        INVOICED_TAX_AMOUNT
    }

    public static class N30ValidationResult {
        private final PlAppendix23202355DTO data;
        private final Map<String, Object> validationDetails;

        private N30ValidationResult(PlAppendix23202355DTO data, Map<String, Object> validationDetails) {
            this.data = data;
            this.validationDetails = validationDetails;
        }

        public PlAppendix23202355DTO getData() {
            return data;
        }

        public Map<String, Object> getValidationDetails() {
            return validationDetails;
        }
    }

}
