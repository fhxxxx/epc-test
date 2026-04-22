package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.command.ConfirmStageCommand;
import com.envision.epc.module.taxledger.application.command.CreateLedgerRunCommand;
import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import com.envision.epc.module.taxledger.application.dto.DatalakeExportRowDTO;
import com.envision.epc.module.taxledger.application.dto.LedgerRunDetailDTO;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.StampDutyDetailRowDTO;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileParseStatusEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.domain.LedgerArtifactTypeEnum;
import com.envision.epc.module.taxledger.domain.LedgerGenerateStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRecord;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.LedgerRunModeEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunStage;
import com.envision.epc.module.taxledger.domain.LedgerRunStageStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunTask;
import com.envision.epc.module.taxledger.domain.LedgerRunTriggerEnum;
import com.envision.epc.module.taxledger.domain.ManualActionTypeEnum;
import com.envision.epc.module.taxledger.domain.RunTaskStatusEnum;
import com.envision.epc.module.taxledger.excel.TaxLedgerExcelService;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunArtifactMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunStageMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunTaskMapper;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 台账运行核心服务（DAG节点编排版）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaxLedgerService {
    private static final String SCHEMA_VERSION = "v1";

    private static final List<NodeSpec> DAG = List.of(
            new NodeSpec("N00", 1, "", ManualActionTypeEnum.NONE, "configs,previous-ledger"),
            new NodeSpec("N10", 2, "N00", ManualActionTypeEnum.NONE, "uploads,datalake"),
            new NodeSpec("N20", 3, "N10", ManualActionTypeEnum.NONE, "stamp-duty,uninvoiced-monitor"),
            new NodeSpec("N30", 4, "N10", ManualActionTypeEnum.FILL_SPLIT_BASIS_PL_2320_2355, "pl-appendix-2320-2355"),
            new NodeSpec("N40", 5, "N20,N30", ManualActionTypeEnum.FILL_PREVIOUS_MONTH_INVOICED_AMOUNT, "vat-change"),
            new NodeSpec("N50", 6, "N40", ManualActionTypeEnum.NONE, "summary"),
            new NodeSpec("N60", 7, "N50", ManualActionTypeEnum.NONE, "cumulative-and-monitor"),
            new NodeSpec("N70", 8, "N60", ManualActionTypeEnum.FILL_DIFFERENCE_ANALYSIS_AND_REASON, "final-manual-check")
    );

    private final LedgerRecordMapper ledgerRecordMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunStageMapper stageMapper;
    private final LedgerRunArtifactMapper artifactMapper;
    private final LedgerRunTaskMapper taskMapper;
    private final FileRecordMapper fileRecordMapper;
    private final BlobStorageRemote blobStorageRemote;
    private final FileParseOrchestratorService fileParseOrchestratorService;
    private final ParsedResultReader parsedResultReader;
    private final TaxLedgerExcelService excelService;
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
        run.setModeSnapshot(command.getMode() == null ? LedgerRunModeEnum.AUTO : command.getMode());
        run.setStatus(LedgerRunStatusEnum.RUNNING);
        run.setCurrentBatch(1);
        run.setInputFingerprint(UUID.randomUUID().toString().replace("-", ""));
        run.setStartedAt(LocalDateTime.now());
        run.setIsDeleted(0);
        ledgerRunMapper.insert(run);

        ledger.setGenerateStatus(LedgerGenerateStatusEnum.PENDING);
        ledger.setStatusMsg("Run started");
        ledgerRecordMapper.updateById(ledger);

        createStages(run.getId());
        createTasks(run.getId());
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

        List<LedgerRunStage> stages = stageMapper.selectList(new LambdaQueryWrapper<LedgerRunStage>()
                .eq(LedgerRunStage::getIsDeleted, 0)
                .eq(LedgerRunStage::getRunId, runId)
                .orderByAsc(LedgerRunStage::getBatchNo));

        List<LedgerRunArtifact> artifacts = artifactMapper.selectList(new LambdaQueryWrapper<LedgerRunArtifact>()
                .eq(LedgerRunArtifact::getIsDeleted, 0)
                .eq(LedgerRunArtifact::getRunId, runId)
                .orderByAsc(LedgerRunArtifact::getBatchNo));

        List<LedgerRunTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<LedgerRunTask>()
                .eq(LedgerRunTask::getIsDeleted, 0)
                .eq(LedgerRunTask::getRunId, runId)
                .orderByAsc(LedgerRunTask::getBatchNo));

        LedgerRunDetailDTO.BlockingManualAction blocking = buildBlockingManualAction(tasks);
        List<String> nextRunnable = findRunnableNodes(tasks);

        return LedgerRunDetailDTO.of(run, stages, artifacts, tasks, blocking, nextRunnable);
    }

    /**
     * GATED 模式人工确认后继续运行
     */
    @Transactional(rollbackFor = Exception.class)
    public LedgerRunDetailDTO confirm(Long runId, ConfirmStageCommand command) {
        LedgerRun run = ledgerRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Run not found");
        }
        if (run.getStatus() != LedgerRunStatusEnum.PAUSED) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Run is not paused");
        }

        LedgerRunTask blockedTask = taskMapper.selectOne(new LambdaQueryWrapper<LedgerRunTask>()
                .eq(LedgerRunTask::getIsDeleted, 0)
                .eq(LedgerRunTask::getRunId, runId)
                .eq(LedgerRunTask::getStatus, RunTaskStatusEnum.BLOCKED_MANUAL)
                .orderByAsc(LedgerRunTask::getBatchNo)
                .last("LIMIT 1"));

        if (blockedTask == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "No blocked manual task found");
        }
        if (command.getBatchNo() != null && !Objects.equals(command.getBatchNo(), blockedTask.getBatchNo())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "batchNo does not match blocked task");
        }

        LedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
        validateManualAction(ledger, blockedTask);

        blockedTask.setStatus(RunTaskStatusEnum.SUCCESS);
        blockedTask.setErrorMsg(null);
        blockedTask.setEndedAt(LocalDateTime.now());
        taskMapper.updateById(blockedTask);
        markStageConfirmed(runId, blockedTask.getBatchNo());

        writeNodeOutput(ledger, run, blockedTask, Map.of(
                "confirmedBy", permissionService.currentUserCode(),
                "action", blockedTask.getManualActionType().name(),
                "confirmedAt", LocalDateTime.now().toString()
        ));

        run.setStatus(LedgerRunStatusEnum.RUNNING);
        run.setCurrentBatch(blockedTask.getBatchNo());
        ledgerRunMapper.updateById(run);

        dispatchRun(runId);
        return getRunDetail(runId);
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
            while (true) {
                LedgerRun run = ledgerRunMapper.selectById(runId);
                if (run == null || run.getIsDeleted() == 1 || run.getStatus() != LedgerRunStatusEnum.RUNNING) {
                    return;
                }

                LedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
                List<LedgerRunTask> tasks = loadTasks(runId);

                if (tasks.stream().allMatch(t -> t.getStatus() == RunTaskStatusEnum.SUCCESS || t.getStatus() == RunTaskStatusEnum.SKIPPED)) {
                    finalizeRunSuccess(run, ledger);
                    return;
                }

                LedgerRunTask blocked = tasks.stream()
                        .filter(t -> t.getStatus() == RunTaskStatusEnum.BLOCKED_MANUAL)
                        .findFirst()
                        .orElse(null);
                if (blocked != null) {
                    run.setStatus(LedgerRunStatusEnum.PAUSED);
                    run.setCurrentBatch(blocked.getBatchNo());
                    ledgerRunMapper.updateById(run);
                    return;
                }

                List<LedgerRunTask> runnable = tasks.stream()
                        .filter(t -> t.getStatus() == RunTaskStatusEnum.PENDING)
                        .filter(t -> dependenciesSatisfied(t, tasks))
                        .sorted(Comparator.comparing(LedgerRunTask::getBatchNo).thenComparing(LedgerRunTask::getNodeCode))
                        .toList();

                if (runnable.isEmpty()) {
                    return;
                }

                for (LedgerRunTask task : runnable) {
                    if (!executeTask(run, ledger, task)) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            markRunFailed(runId, e.getMessage());
            log.error("run process failed, runId={}", runId, e);
        }
    }

    private boolean executeTask(LedgerRun run, LedgerRecord ledger, LedgerRunTask task) {
        task.setStatus(RunTaskStatusEnum.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        markStageRunning(run.getId(), task.getBatchNo());

        try {
            if (shouldBlockForManual(run, task)) {
                task.setStatus(RunTaskStatusEnum.BLOCKED_MANUAL);
                task.setEndedAt(LocalDateTime.now());
                task.setErrorMsg("Waiting for manual action: " + task.getManualActionType().name());
                taskMapper.updateById(task);

                markStageBlocked(run.getId(), task.getBatchNo(), task.getErrorMsg());
                run.setStatus(LedgerRunStatusEnum.PAUSED);
                run.setCurrentBatch(task.getBatchNo());
                ledgerRunMapper.updateById(run);
                return false;
            }

            Map<String, Object> output = executeNode(run, ledger, task);

            writeNodeOutput(ledger, run, task, output);

            task.setStatus(RunTaskStatusEnum.SUCCESS);
            task.setErrorMsg(null);
            task.setEndedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            markStageSuccess(run.getId(), task.getBatchNo());
            run.setCurrentBatch(task.getBatchNo());
            ledgerRunMapper.updateById(run);
            return true;
        } catch (Exception e) {
            task.setStatus(RunTaskStatusEnum.FAILED);
            task.setErrorMsg(e.getMessage());
            task.setEndedAt(LocalDateTime.now());
            taskMapper.updateById(task);
            markStageFailed(run.getId(), task.getBatchNo(), e.getMessage());
            markRunFailed(run.getId(), e.getMessage());
            return false;
        }
    }

    private Map<String, Object> executeNode(LedgerRun run, LedgerRecord ledger, LedgerRunTask task) {
        return switch (task.getNodeCode()) {
            case "N10" -> executeN10(ledger);
            case "N20" -> executeN20(run, ledger);
            default -> defaultNodeOutput(task);
        };
    }

    private Map<String, Object> defaultNodeOutput(LedgerRunTask task) {
        Map<String, Object> output = new HashMap<>();
        output.put("message", "Node completed");
        output.put("nodeCode", task.getNodeCode());
        output.put("batchNo", task.getBatchNo());
        output.put("inputRefs", task.getInputRefs());
        output.put("timestamp", LocalDateTime.now().toString());
        return output;
    }

    private Map<String, Object> executeN10(LedgerRecord ledger) {
        List<FileRecord> files = loadFiles(ledger.getCompanyCode(), ledger.getYearMonth());
        Map<FileCategoryEnum, FileRecord> latestByCategory = latestFileByCategory(files);
        Set<FileCategoryEnum> required = requiredCategoriesForN20(ledger.getCompanyCode());

        List<String> missingCategories = required.stream()
                .filter(category -> !latestByCategory.containsKey(category))
                .map(Enum::name)
                .sorted()
                .toList();
        if (!missingCategories.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "N10 missing required categories: " + String.join(",", missingCategories));
        }

        for (FileCategoryEnum category : required) {
            FileRecord file = latestByCategory.get(category);
            FileRecord refreshed = ensureParseResultReady(file);
            latestByCategory.put(category, refreshed);
        }

        List<Map<String, Object>> inputs = latestByCategory.values().stream()
                .sorted(Comparator.comparing(record -> record.getFileCategory().name()))
                .map(this::toN10InputItem)
                .toList();

        long readyRequired = required.stream()
                .map(latestByCategory::get)
                .filter(Objects::nonNull)
                .filter(record -> record.getParseStatus() == FileParseStatusEnum.SUCCESS
                        && record.getParseResultBlobPath() != null
                        && !record.getParseResultBlobPath().isBlank())
                .count();

        long readyTotal = latestByCategory.values().stream()
                .filter(record -> record.getParseStatus() == FileParseStatusEnum.SUCCESS
                        && record.getParseResultBlobPath() != null
                        && !record.getParseResultBlobPath().isBlank())
                .count();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("requiredReadyCount", readyRequired);
        summary.put("totalReadyCount", readyTotal);
        summary.put("missingCategories", missingCategories);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("companyCode", ledger.getCompanyCode());
        output.put("yearMonth", ledger.getYearMonth());
        output.put("requiredCategories", required.stream().map(Enum::name).sorted().toList());
        output.put("inputs", inputs);
        output.put("summary", summary);
        return output;
    }

    private Map<String, Object> executeN20(LedgerRun run, LedgerRecord ledger) {
        N10Snapshot n10 = loadN10Snapshot(run.getId());

        List<PlStatementRowDTO> plRows = readRequiredParsedList(n10, FileCategoryEnum.PL, PlStatementRowDTO.class);
        List<DatalakeExportRowDTO> dlOtherRows = readRequiredParsedList(n10, FileCategoryEnum.DL_OTHER, DatalakeExportRowDTO.class);
        List<DatalakeExportRowDTO> dlOutputRows = readRequiredParsedList(n10, FileCategoryEnum.DL_OUTPUT, DatalakeExportRowDTO.class);
        VatOutputSheetUploadDTO vatOutput = readRequiredParsedObject(n10, FileCategoryEnum.VAT_OUTPUT, VatOutputSheetUploadDTO.class);

        Map<String, Object> stampDutyBlock = buildStampDutyPrecompute(ledger.getCompanyCode(), ledger.getYearMonth(), n10);
        Map<String, Object> uninvoicedBlock = buildUninvoicedPrecompute(run, ledger, plRows, dlOtherRows, dlOutputRows, vatOutput);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("stampDutyNon23202355", stampDutyBlock);
        output.put("uninvoicedMonitor", uninvoicedBlock);
        return output;
    }

    private Set<FileCategoryEnum> requiredCategoriesForN20(String companyCode) {
        Set<FileCategoryEnum> required = new HashSet<>();
        required.add(FileCategoryEnum.PL);
        required.add(FileCategoryEnum.DL_OTHER);
        required.add(FileCategoryEnum.DL_OUTPUT);
        required.add(FileCategoryEnum.VAT_OUTPUT);
        if (!isCompany2320Or2355(companyCode)) {
            required.add(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER);
        }
        return required;
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

    private FileRecord ensureParseResultReady(FileRecord file) {
        if (file == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "file record is null");
        }

        boolean ready = file.getParseStatus() == FileParseStatusEnum.SUCCESS
                && file.getParseResultBlobPath() != null
                && !file.getParseResultBlobPath().isBlank();
        if (!ready) {
            fileParseOrchestratorService.loadParsedResultOrParse(file, "system");
        }

        FileRecord refreshed = fileRecordMapper.selectById(file.getId());
        if (refreshed == null || refreshed.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "file not found after parse: " + file.getId());
        }
        if (refreshed.getParseStatus() != FileParseStatusEnum.SUCCESS
                || refreshed.getParseResultBlobPath() == null
                || refreshed.getParseResultBlobPath().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "required file parse failed: " + refreshed.getFileCategory().name());
        }
        return refreshed;
    }

    private Map<String, Object> toN10InputItem(FileRecord record) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("fileId", record.getId());
        item.put("fileName", record.getFileName());
        item.put("fileCategory", record.getFileCategory() == null ? null : record.getFileCategory().name());
        item.put("parseStatus", record.getParseStatus() == null ? null : record.getParseStatus().name());
        item.put("parseResultBlobPath", record.getParseResultBlobPath());
        item.put("fileSize", record.getFileSize());
        return item;
    }

    private N10Snapshot loadN10Snapshot(Long runId) {
        LedgerRunTask n10Task = taskMapper.selectOne(new LambdaQueryWrapper<LedgerRunTask>()
                .eq(LedgerRunTask::getRunId, runId)
                .eq(LedgerRunTask::getNodeCode, "N10")
                .eq(LedgerRunTask::getIsDeleted, 0)
                .orderByDesc(LedgerRunTask::getId)
                .last("LIMIT 1"));
        if (n10Task == null || n10Task.getOutputBlobPath() == null || n10Task.getOutputBlobPath().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "N10 output not found");
        }
        N10Snapshot snapshot = parsedResultReader.readNodeOutputData(n10Task.getOutputBlobPath(), N10Snapshot.class);
        if (snapshot == null || snapshot.getInputs() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "N10 output is invalid");
        }
        return snapshot;
    }

    private <T> List<T> readRequiredParsedList(N10Snapshot snapshot, FileCategoryEnum category, Class<T> itemType) {
        N10InputItem item = snapshot.findByCategory(category);
        if (item == null || item.getParseResultBlobPath() == null || item.getParseResultBlobPath().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "N20 missing parsed input: " + category.name());
        }
        return parsedResultReader.readParsedList(item.getParseResultBlobPath(), itemType);
    }

    private <T> T readRequiredParsedObject(N10Snapshot snapshot, FileCategoryEnum category, Class<T> type) {
        N10InputItem item = snapshot.findByCategory(category);
        if (item == null || item.getParseResultBlobPath() == null || item.getParseResultBlobPath().isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "N20 missing parsed input: " + category.name());
        }
        return parsedResultReader.readParsedData(item.getParseResultBlobPath(), type);
    }

    private Map<String, Object> buildStampDutyPrecompute(String companyCode, String yearMonth, N10Snapshot n10) {
        Map<String, Object> block = new LinkedHashMap<>();
        if (isCompany2320Or2355(companyCode)) {
            block.put("skipped", true);
            block.put("reason", "company is 2320/2355");
            block.put("rows", List.of());
            return block;
        }

        List<ContractStampDutyLedgerItemDTO> contractRows =
                readRequiredParsedList(n10, FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER, ContractStampDutyLedgerItemDTO.class);
        String quarter = resolveQuarter(yearMonth);

        Map<String, StampDutyAggregate> agg = new LinkedHashMap<>();
        for (ContractStampDutyLedgerItemDTO row : contractRows) {
            if (row == null) {
                continue;
            }
            if (!quarter.equals(normalizeText(row.getQuarter()))) {
                continue;
            }
            String taxItem = normalizeText(row.getStampDutyTaxItem());
            if (taxItem == null || taxItem.isBlank()) {
                continue;
            }
            StampDutyAggregate current = agg.computeIfAbsent(taxItem, key -> new StampDutyAggregate());
            current.taxableBasis = add(current.taxableBasis, row.getContractAmount());
            if (current.taxRate == null) {
                current.taxRate = row.getStampDutyTaxRate();
            }
            current.taxPayableAmount = add(current.taxPayableAmount, row.getTaxableAmount());
            BigDecimal reduction = multiply(row.getContractAmount(), row.getStampDutyTaxRate(), row.getPreferentialRatio());
            current.taxReductionAmount = add(current.taxReductionAmount, reduction);
        }

        List<StampDutyDetailRowDTO> rows = new ArrayList<>();
        int index = 1;
        BigDecimal totalTaxableBasis = BigDecimal.ZERO;
        BigDecimal totalPayable = BigDecimal.ZERO;
        BigDecimal totalReduction = BigDecimal.ZERO;
        BigDecimal totalRefundable = BigDecimal.ZERO;
        for (Map.Entry<String, StampDutyAggregate> entry : agg.entrySet()) {
            StampDutyAggregate item = entry.getValue();
            StampDutyDetailRowDTO row = new StampDutyDetailRowDTO();
            row.setSerialNo(String.valueOf(index++));
            row.setTaxItem(entry.getKey());
            row.setTaxableBasis(zeroToNull(item.taxableBasis));
            row.setTaxRate(item.taxRate);
            row.setTaxPayableAmount(zeroToNull(item.taxPayableAmount));
            row.setTaxReductionAmount(zeroToNull(item.taxReductionAmount));
            row.setTaxPaidAmount(null);
            row.setTaxPayableOrRefundableAmount(zeroToNull(subtract(item.taxPayableAmount, item.taxReductionAmount)));
            rows.add(row);

            totalTaxableBasis = add(totalTaxableBasis, row.getTaxableBasis());
            totalPayable = add(totalPayable, row.getTaxPayableAmount());
            totalReduction = add(totalReduction, row.getTaxReductionAmount());
            totalRefundable = add(totalRefundable, row.getTaxPayableOrRefundableAmount());
        }

        if (!rows.isEmpty()) {
            StampDutyDetailRowDTO total = new StampDutyDetailRowDTO();
            total.setSerialNo("合计");
            total.setTaxItem("");
            total.setTaxableBasis(zeroToNull(totalTaxableBasis));
            total.setTaxRate(null);
            total.setTaxPayableAmount(zeroToNull(totalPayable));
            total.setTaxReductionAmount(zeroToNull(totalReduction));
            total.setTaxPaidAmount(null);
            total.setTaxPayableOrRefundableAmount(zeroToNull(totalRefundable));
            rows.add(total);
        }

        block.put("skipped", false);
        block.put("quarter", quarter);
        block.put("rows", rows);
        return block;
    }

    private Map<String, Object> buildUninvoicedPrecompute(LedgerRun run,
                                                          LedgerRecord ledger,
                                                          List<PlStatementRowDTO> plRows,
                                                          List<DatalakeExportRowDTO> dlOtherRows,
                                                          List<DatalakeExportRowDTO> dlOutputRows,
                                                          VatOutputSheetUploadDTO vatOutput) {
        List<UninvoicedMonitorItemDTO> baseRows = loadPreviousUninvoicedRows(ledger.getCompanyCode(), ledger.getYearMonth());
        String currentPeriod = toPeriodLabel(ledger.getYearMonth());
        baseRows.removeIf(row -> currentPeriod.equals(normalizeText(row.getPeriod())));

        UninvoicedMonitorItemDTO current = new UninvoicedMonitorItemDTO();
        current.setPeriod(currentPeriod);
        current.setDeclaredMainBusinessRevenue(sumPlCurrentAmount(plRows, "主营业务收入"));
        current.setDeclaredInterestIncome(sumDatalakeByAccount(dlOtherRows, "6603020011"));
        current.setDeclaredOtherIncome(sumDatalakeByAccount(dlOtherRows, "6702000010"));
        current.setDeclaredOutputTax(negate(sumDatalakeDocumentAmount(dlOutputRows)));

        BigDecimal invoicedSalesIncome = sumVatOutputByTotal(vatOutput, true);
        BigDecimal invoicedOutputTax = sumVatOutputByTotal(vatOutput, false);
        current.setInvoicedSalesIncome(invoicedSalesIncome);
        current.setInvoicedOutputTax(invoicedOutputTax);
        current.setUninvoicedSalesIncome(subtract(add(add(current.getDeclaredMainBusinessRevenue(),
                current.getDeclaredInterestIncome()), current.getDeclaredOtherIncome()), current.getInvoicedSalesIncome()));
        current.setUninvoicedOutputTax(subtract(current.getDeclaredOutputTax(), current.getInvoicedOutputTax()));
        baseRows.add(current);
        baseRows.sort(Comparator.comparing(row -> periodSortKey(row.getPeriod())));

        UninvoicedMonitorItemDTO totalRow = buildUninvoicedTotalRow(baseRows);
        List<UninvoicedMonitorItemDTO> rowsWithTotal = new ArrayList<>(baseRows);
        rowsWithTotal.add(totalRow);

        Map<String, Object> block = new LinkedHashMap<>();
        block.put("runId", run.getId());
        block.put("rows", rowsWithTotal);
        block.put("detailCount", baseRows.size());
        return block;
    }

    private List<UninvoicedMonitorItemDTO> loadPreviousUninvoicedRows(String companyCode, String yearMonth) {
        YearMonth current = parseYearMonth(yearMonth);
        YearMonth previous = current.minusMonths(1);
        List<String> candidates = List.of(previous.toString().replace("-", ""), previous.toString());
        for (String candidate : candidates) {
            LedgerRecord record = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<LedgerRecord>()
                    .eq(LedgerRecord::getIsDeleted, 0)
                    .eq(LedgerRecord::getCompanyCode, companyCode)
                    .eq(LedgerRecord::getYearMonth, candidate)
                    .last("LIMIT 1"));
            if (record == null) {
                continue;
            }
            LedgerRun run = ledgerRunMapper.selectOne(new LambdaQueryWrapper<LedgerRun>()
                    .eq(LedgerRun::getIsDeleted, 0)
                    .eq(LedgerRun::getLedgerId, record.getId())
                    .eq(LedgerRun::getStatus, LedgerRunStatusEnum.SUCCESS)
                    .orderByDesc(LedgerRun::getRunNo)
                    .last("LIMIT 1"));
            if (run == null) {
                continue;
            }
            LedgerRunTask n20Task = taskMapper.selectOne(new LambdaQueryWrapper<LedgerRunTask>()
                    .eq(LedgerRunTask::getIsDeleted, 0)
                    .eq(LedgerRunTask::getRunId, run.getId())
                    .eq(LedgerRunTask::getNodeCode, "N20")
                    .eq(LedgerRunTask::getStatus, RunTaskStatusEnum.SUCCESS)
                    .orderByDesc(LedgerRunTask::getId)
                    .last("LIMIT 1"));
            if (n20Task == null || n20Task.getOutputBlobPath() == null || n20Task.getOutputBlobPath().isBlank()) {
                continue;
            }
            try {
                N20Snapshot snapshot = parsedResultReader.readNodeOutputData(n20Task.getOutputBlobPath(), N20Snapshot.class);
                if (snapshot == null || snapshot.getUninvoicedMonitor() == null || snapshot.getUninvoicedMonitor().getRows() == null) {
                    continue;
                }
                return snapshot.getUninvoicedMonitor().getRows().stream()
                        .filter(row -> row != null && !"合计数".equals(normalizeText(row.getPeriod())))
                        .collect(Collectors.toCollection(ArrayList::new));
            } catch (Exception e) {
                log.warn("load previous uninvoiced rows failed, company={}, yearMonth={}", companyCode, candidate, e);
            }
        }
        return new ArrayList<>();
    }

    private String resolveQuarter(String yearMonth) {
        int month = parseYearMonth(yearMonth).getMonthValue();
        if (month <= 3) {
            return "Q1";
        }
        if (month <= 6) {
            return "Q2";
        }
        if (month <= 9) {
            return "Q3";
        }
        return "Q4";
    }

    private YearMonth parseYearMonth(String yearMonth) {
        String normalized = normalizeText(yearMonth);
        if (normalized == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "yearMonth is blank");
        }
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

    private int periodSortKey(String period) {
        String text = normalizeText(period);
        if (text == null || !text.matches("^\\d{4}-\\d{1,2}$")) {
            return Integer.MAX_VALUE;
        }
        String[] parts = text.split("-");
        return Integer.parseInt(parts[0]) * 100 + Integer.parseInt(parts[1]);
    }

    private BigDecimal sumPlCurrentAmount(List<PlStatementRowDTO> rows, String itemName) {
        BigDecimal sum = BigDecimal.ZERO;
        for (PlStatementRowDTO row : rows) {
            if (row == null || normalizeText(row.getItemName()) == null) {
                continue;
            }
            if (normalizeText(row.getItemName()).equals(itemName)) {
                sum = add(sum, row.getCurrentPeriodAmount());
            }
        }
        return sum;
    }

    private BigDecimal sumDatalakeByAccount(List<DatalakeExportRowDTO> rows, String account) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (account.equals(normalizeText(row.getAccount()))) {
                sum = add(sum, toBigDecimal(row.getDocumentAmount()));
            }
        }
        return sum;
    }

    private BigDecimal sumDatalakeDocumentAmount(List<DatalakeExportRowDTO> rows) {
        BigDecimal sum = BigDecimal.ZERO;
        for (DatalakeExportRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            sum = add(sum, toBigDecimal(row.getDocumentAmount()));
        }
        return sum;
    }

    private BigDecimal sumVatOutputByTotal(VatOutputSheetUploadDTO vatOutput, boolean amount) {
        if (vatOutput == null || vatOutput.getTaxRateSummaries() == null) {
            return BigDecimal.ZERO;
        }
        List<VatOutputSheetUploadDTO.TaxRateSummaryItem> rows = vatOutput.getTaxRateSummaries();
        List<VatOutputSheetUploadDTO.TaxRateSummaryItem> totalRows = rows.stream()
                .filter(Objects::nonNull)
                .filter(item -> {
                    String status = normalizeText(item.getInvoiceStatus());
                    return status != null && status.contains("合计");
                })
                .toList();
        List<VatOutputSheetUploadDTO.TaxRateSummaryItem> source = totalRows.isEmpty() ? rows : totalRows;
        BigDecimal sum = BigDecimal.ZERO;
        for (VatOutputSheetUploadDTO.TaxRateSummaryItem item : source) {
            if (item == null) {
                continue;
            }
            sum = add(sum, amount ? item.getBlueInvoiceAmount() : item.getBlueInvoiceTaxAmount());
        }
        return sum;
    }

    private UninvoicedMonitorItemDTO buildUninvoicedTotalRow(List<UninvoicedMonitorItemDTO> detailRows) {
        UninvoicedMonitorItemDTO total = new UninvoicedMonitorItemDTO();
        total.setPeriod("合计数");
        for (UninvoicedMonitorItemDTO row : detailRows) {
            total.setDeclaredMainBusinessRevenue(add(total.getDeclaredMainBusinessRevenue(), row.getDeclaredMainBusinessRevenue()));
            total.setDeclaredInterestIncome(add(total.getDeclaredInterestIncome(), row.getDeclaredInterestIncome()));
            total.setDeclaredOtherIncome(add(total.getDeclaredOtherIncome(), row.getDeclaredOtherIncome()));
            total.setDeclaredOutputTax(add(total.getDeclaredOutputTax(), row.getDeclaredOutputTax()));
            total.setInvoicedSalesIncome(add(total.getInvoicedSalesIncome(), row.getInvoicedSalesIncome()));
            total.setInvoicedOutputTax(add(total.getInvoicedOutputTax(), row.getInvoicedOutputTax()));
            total.setUninvoicedSalesIncome(add(total.getUninvoicedSalesIncome(), row.getUninvoicedSalesIncome()));
            total.setUninvoicedOutputTax(add(total.getUninvoicedOutputTax(), row.getUninvoicedOutputTax()));
        }
        return total;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        return value.trim();
    }

    private BigDecimal toBigDecimal(String raw) {
        String text = normalizeText(raw);
        if (text == null || text.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            String normalized = text.replace(",", "");
            if (normalized.startsWith("(") && normalized.endsWith(")")) {
                normalized = "-" + normalized.substring(1, normalized.length() - 1);
            }
            return new BigDecimal(normalized);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal multiply(BigDecimal a, BigDecimal b, BigDecimal c) {
        if (a == null || b == null || c == null) {
            return BigDecimal.ZERO;
        }
        return a.multiply(b).multiply(c).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal add(BigDecimal a, BigDecimal b) {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        BigDecimal right = b == null ? BigDecimal.ZERO : b;
        return left.add(right);
    }

    private BigDecimal subtract(BigDecimal a, BigDecimal b) {
        BigDecimal left = a == null ? BigDecimal.ZERO : a;
        BigDecimal right = b == null ? BigDecimal.ZERO : b;
        return left.subtract(right);
    }

    private BigDecimal negate(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        return value.negate();
    }

    private BigDecimal zeroToNull(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.ZERO.compareTo(value) == 0 ? null : value;
    }

    private void finalizeRunSuccess(LedgerRun run, LedgerRecord ledger) throws Exception {
        List<FileRecord> files = loadFiles(ledger.getCompanyCode(), ledger.getYearMonth());
        List<LedgerRunTask> tasks = loadTasks(run.getId());
        Map<String, Object> nodeOutputs = loadNodeOutputs(tasks);

        byte[] finalLedger = excelService.buildLedger(ledger.getCompanyCode(), ledger.getYearMonth(), files, nodeOutputs);
        String blobPath = String.format("tax-ledger/%s/%s/final/%s",
                ledger.getCompanyCode(), ledger.getYearMonth(), ledger.getLedgerName());
        blobStorageRemote.upload(blobPath, new ByteArrayInputStream(finalLedger));
        saveArtifact(run.getId(), 8, LedgerArtifactTypeEnum.FINAL_LEDGER, ledger.getLedgerName(), blobPath,
                (long) finalLedger.length, sha256(finalLedger));

        run.setStatus(LedgerRunStatusEnum.SUCCESS);
        run.setEndedAt(LocalDateTime.now());
        ledgerRunMapper.updateById(run);

        ledger.setGenerateStatus(LedgerGenerateStatusEnum.SUCCESS);
        ledger.setGeneratedAt(LocalDateTime.now());
        ledger.setStatusMsg("Success");
        ledger.setBlobPath(blobPath);
        ledgerRecordMapper.updateById(ledger);
    }

    private Map<String, Object> loadNodeOutputs(List<LedgerRunTask> tasks) {
        Map<String, Object> outputs = new HashMap<>();
        for (LedgerRunTask task : tasks) {
            if (task.getOutputBlobPath() == null || task.getOutputBlobPath().isBlank()) {
                continue;
            }
            try {
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                blobStorageRemote.loadStream(task.getOutputBlobPath(), output);
                Map<?, ?> node = objectMapper.readValue(output.toByteArray(), Map.class);
                outputs.put(task.getNodeCode(), node);
            } catch (Exception e) {
                outputs.put(task.getNodeCode(), Map.of("error", e.getMessage()));
            }
        }
        return outputs;
    }

    private boolean shouldBlockForManual(LedgerRun run, LedgerRunTask task) {
        return run.getModeSnapshot() == LedgerRunModeEnum.GATED
                && task.getManualActionType() != null
                && task.getManualActionType() != ManualActionTypeEnum.NONE;
    }

    private void validateManualAction(LedgerRecord ledger, LedgerRunTask blockedTask) {
        ManualActionTypeEnum action = blockedTask.getManualActionType();
        if (action == null || action == ManualActionTypeEnum.NONE) {
            return;
        }

        List<FileRecord> files = loadFiles(ledger.getCompanyCode(), ledger.getYearMonth());
        switch (action) {
            case FILL_SPLIT_BASIS_PL_2320_2355 -> {
                boolean hasMonthly = files.stream().anyMatch(f -> f.getFileCategory() == FileCategoryEnum.MONTHLY_SETTLEMENT_TAX
                        && f.getParseStatus() == FileParseStatusEnum.SUCCESS);
                if (!hasMonthly) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "Missing parsed file: 睿景景程月结数据表-报税");
                }
            }
            case FILL_PREVIOUS_MONTH_INVOICED_AMOUNT -> {
                boolean hasAppendix = files.stream().anyMatch(f -> f.getFileCategory() == FileCategoryEnum.VAT_CHANGE_APPENDIX);
                if (!hasAppendix) {
                    throw new BizException(ErrorCode.BAD_REQUEST,
                            "Missing file: 增值税变动表附表");
                }
            }
            case FILL_DIFFERENCE_ANALYSIS_AND_REASON -> {
                // 允许纯人工确认继续
            }
            default -> {
            }
        }
    }

    private void writeNodeOutput(LedgerRecord ledger, LedgerRun run, LedgerRunTask task, Map<String, Object> outputData) {
        try {
            Map<String, Object> envelope = new HashMap<>();
            envelope.put("schemaVersion", SCHEMA_VERSION);
            envelope.put("runId", run.getId());
            envelope.put("nodeCode", task.getNodeCode());
            envelope.put("inputRefs", task.getInputRefs());
            envelope.put("outputData", outputData);
            envelope.put("validations", List.of());
            envelope.put("generatedAt", LocalDateTime.now());

            byte[] bytes = objectMapper.writeValueAsBytes(envelope);
            String path = String.format("tax-ledger/%s/%s/runs/%d/nodes/%s.json",
                    ledger.getCompanyCode(), ledger.getYearMonth(), run.getId(), task.getNodeCode());
            blobStorageRemote.upload(path, new ByteArrayInputStream(bytes));

            task.setOutputBlobPath(path);
            taskMapper.updateById(task);
            saveArtifact(run.getId(), task.getBatchNo(), LedgerArtifactTypeEnum.NODE_OUTPUT_JSON,
                    task.getNodeCode() + ".json", path, (long) bytes.length, sha256(bytes));
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "write node output failed: " + e.getMessage());
        }
    }

    private List<String> listUploadedCategories(String companyCode, String yearMonth) {
        return loadFiles(companyCode, yearMonth).stream()
                .map(f -> f.getFileCategory().name())
                .distinct()
                .sorted()
                .toList();
    }

    private List<FileRecord> loadFiles(String companyCode, String yearMonth) {
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, yearMonth));
    }

    private List<LedgerRunTask> loadTasks(Long runId) {
        return taskMapper.selectList(new LambdaQueryWrapper<LedgerRunTask>()
                .eq(LedgerRunTask::getIsDeleted, 0)
                .eq(LedgerRunTask::getRunId, runId));
    }

    private boolean dependenciesSatisfied(LedgerRunTask task, List<LedgerRunTask> allTasks) {
        if (task.getDependsOn() == null || task.getDependsOn().isBlank()) {
            return true;
        }
        Set<String> deps = Set.of(task.getDependsOn().split(","));
        Set<String> successNodes = allTasks.stream()
                .filter(t -> t.getStatus() == RunTaskStatusEnum.SUCCESS || t.getStatus() == RunTaskStatusEnum.SKIPPED)
                .map(LedgerRunTask::getNodeCode)
                .collect(Collectors.toSet());
        return successNodes.containsAll(deps);
    }

    private LedgerRunDetailDTO.BlockingManualAction buildBlockingManualAction(List<LedgerRunTask> tasks) {
        LedgerRunTask blocked = tasks.stream()
                .filter(t -> t.getStatus() == RunTaskStatusEnum.BLOCKED_MANUAL)
                .findFirst()
                .orElse(null);
        if (blocked == null) {
            return null;
        }

        LedgerRunDetailDTO.BlockingManualAction action = new LedgerRunDetailDTO.BlockingManualAction();
        action.setActionCode(blocked.getManualActionType());

        List<String> requiredFields = new ArrayList<>();
        String hint = "请完成人工补录后点击继续执行";
        if (blocked.getManualActionType() == ManualActionTypeEnum.FILL_SPLIT_BASIS_PL_2320_2355) {
            requiredFields = List.of("splitBasis", "taxRate", "sectionConsistency");
            hint = "请在 PL附表-2320、2355 补齐拆分依据并确保税率可提取";
        } else if (blocked.getManualActionType() == ManualActionTypeEnum.FILL_PREVIOUS_MONTH_INVOICED_AMOUNT) {
            requiredFields = List.of("previousMonthInvoicedAmount");
            hint = "请在增值税变动表补录以前月度开票金额";
        } else if (blocked.getManualActionType() == ManualActionTypeEnum.FILL_DIFFERENCE_ANALYSIS_AND_REASON) {
            requiredFields = List.of("differenceAnalysis", "reason");
            hint = "请补录差异分析与原因";
        }
        action.setRequiredFields(requiredFields);
        action.setHint(hint);
        return action;
    }

    private List<String> findRunnableNodes(List<LedgerRunTask> tasks) {
        return tasks.stream()
                .filter(t -> t.getStatus() == RunTaskStatusEnum.PENDING)
                .filter(t -> dependenciesSatisfied(t, tasks))
                .sorted(Comparator.comparing(LedgerRunTask::getBatchNo).thenComparing(LedgerRunTask::getNodeCode))
                .map(LedgerRunTask::getNodeCode)
                .toList();
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

    private void markStageRunning(Long runId, int batchNo) {
        LedgerRunStage stage = getStage(runId, batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.RUNNING);
        stage.setStartedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
    }

    private void markStageBlocked(Long runId, int batchNo, String msg) {
        LedgerRunStage stage = getStage(runId, batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.BLOCKED_MANUAL);
        stage.setErrorMsg(msg);
        stageMapper.updateById(stage);
    }

    private void markStageSuccess(Long runId, int batchNo) {
        LedgerRunStage stage = getStage(runId, batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.SUCCESS);
        stage.setSheetCountTotal(1);
        stage.setSheetCountSuccess(1);
        stage.setEndedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
    }

    private void markStageConfirmed(Long runId, int batchNo) {
        LedgerRunStage stage = getStage(runId, batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.CONFIRMED);
        stage.setConfirmUser(permissionService.currentUserCode());
        stage.setConfirmTime(LocalDateTime.now());
        stageMapper.updateById(stage);
    }

    private void markStageFailed(Long runId, int batchNo, String msg) {
        LedgerRunStage stage = getStage(runId, batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.FAILED);
        stage.setErrorMsg(msg);
        stage.setEndedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
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

    private LedgerRunStage getStage(Long runId, int batchNo) {
        LedgerRunStage stage = stageMapper.selectOne(new LambdaQueryWrapper<LedgerRunStage>()
                .eq(LedgerRunStage::getRunId, runId)
                .eq(LedgerRunStage::getBatchNo, batchNo)
                .eq(LedgerRunStage::getIsDeleted, 0));
        if (stage == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Stage not found: " + batchNo);
        }
        return stage;
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

    private void createStages(Long runId) {
        Set<Integer> batches = DAG.stream().map(NodeSpec::batchNo).collect(Collectors.toCollection(HashSet::new));
        for (Integer batchNo : batches.stream().sorted().toList()) {
            LedgerRunStage stage = new LedgerRunStage();
            stage.setRunId(runId);
            stage.setBatchNo(batchNo);
            stage.setStatus(LedgerRunStageStatusEnum.PENDING);
            stage.setDependsOn(batchNo == 1 ? "" : String.valueOf(batchNo - 1));
            stage.setIsDeleted(0);
            stageMapper.insert(stage);
        }
    }

    private void createTasks(Long runId) {
        for (NodeSpec spec : DAG) {
            LedgerRunTask task = new LedgerRunTask();
            task.setRunId(runId);
            task.setNodeCode(spec.nodeCode());
            task.setBatchNo(spec.batchNo());
            task.setStatus(RunTaskStatusEnum.PENDING);
            task.setDependsOn(spec.dependsOn());
            task.setManualActionType(spec.manualActionType());
            task.setInputRefs(spec.inputRefs());
            task.setRetryCount(0);
            task.setIsDeleted(0);
            taskMapper.insert(task);
        }
    }

    private static class StampDutyAggregate {
        private BigDecimal taxableBasis = BigDecimal.ZERO;
        private BigDecimal taxRate;
        private BigDecimal taxPayableAmount = BigDecimal.ZERO;
        private BigDecimal taxReductionAmount = BigDecimal.ZERO;
    }

    public static class N10Snapshot {
        private String companyCode;
        private String yearMonth;
        private List<String> requiredCategories;
        private List<N10InputItem> inputs;
        private Map<String, Object> summary;

        public N10InputItem findByCategory(FileCategoryEnum category) {
            if (inputs == null || category == null) {
                return null;
            }
            return inputs.stream()
                    .filter(Objects::nonNull)
                    .filter(item -> category.name().equals(item.getFileCategory()))
                    .findFirst()
                    .orElse(null);
        }

        public String getCompanyCode() {
            return companyCode;
        }

        public void setCompanyCode(String companyCode) {
            this.companyCode = companyCode;
        }

        public String getYearMonth() {
            return yearMonth;
        }

        public void setYearMonth(String yearMonth) {
            this.yearMonth = yearMonth;
        }

        public List<String> getRequiredCategories() {
            return requiredCategories;
        }

        public void setRequiredCategories(List<String> requiredCategories) {
            this.requiredCategories = requiredCategories;
        }

        public List<N10InputItem> getInputs() {
            return inputs;
        }

        public void setInputs(List<N10InputItem> inputs) {
            this.inputs = inputs;
        }

        public Map<String, Object> getSummary() {
            return summary;
        }

        public void setSummary(Map<String, Object> summary) {
            this.summary = summary;
        }
    }

    public static class N10InputItem {
        private Long fileId;
        private String fileName;
        private String fileCategory;
        private String parseStatus;
        private String parseResultBlobPath;
        private Long fileSize;

        public Long getFileId() {
            return fileId;
        }

        public void setFileId(Long fileId) {
            this.fileId = fileId;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public String getFileCategory() {
            return fileCategory;
        }

        public void setFileCategory(String fileCategory) {
            this.fileCategory = fileCategory;
        }

        public String getParseStatus() {
            return parseStatus;
        }

        public void setParseStatus(String parseStatus) {
            this.parseStatus = parseStatus;
        }

        public String getParseResultBlobPath() {
            return parseResultBlobPath;
        }

        public void setParseResultBlobPath(String parseResultBlobPath) {
            this.parseResultBlobPath = parseResultBlobPath;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
    }

    public static class N20Snapshot {
        private StampDutyBlock stampDutyNon23202355;
        private UninvoicedMonitorBlock uninvoicedMonitor;

        public StampDutyBlock getStampDutyNon23202355() {
            return stampDutyNon23202355;
        }

        public void setStampDutyNon23202355(StampDutyBlock stampDutyNon23202355) {
            this.stampDutyNon23202355 = stampDutyNon23202355;
        }

        public UninvoicedMonitorBlock getUninvoicedMonitor() {
            return uninvoicedMonitor;
        }

        public void setUninvoicedMonitor(UninvoicedMonitorBlock uninvoicedMonitor) {
            this.uninvoicedMonitor = uninvoicedMonitor;
        }
    }

    public static class StampDutyBlock {
        private Boolean skipped;
        private String reason;
        private String quarter;
        private List<StampDutyDetailRowDTO> rows;

        public Boolean getSkipped() {
            return skipped;
        }

        public void setSkipped(Boolean skipped) {
            this.skipped = skipped;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }

        public String getQuarter() {
            return quarter;
        }

        public void setQuarter(String quarter) {
            this.quarter = quarter;
        }

        public List<StampDutyDetailRowDTO> getRows() {
            return rows;
        }

        public void setRows(List<StampDutyDetailRowDTO> rows) {
            this.rows = rows;
        }
    }

    public static class UninvoicedMonitorBlock {
        private Long runId;
        private List<UninvoicedMonitorItemDTO> rows;
        private Integer detailCount;

        public Long getRunId() {
            return runId;
        }

        public void setRunId(Long runId) {
            this.runId = runId;
        }

        public List<UninvoicedMonitorItemDTO> getRows() {
            return rows;
        }

        public void setRows(List<UninvoicedMonitorItemDTO> rows) {
            this.rows = rows;
        }

        public Integer getDetailCount() {
            return detailCount;
        }

        public void setDetailCount(Integer detailCount) {
            this.detailCount = detailCount;
        }
    }

    private record NodeSpec(String nodeCode,
                            Integer batchNo,
                            String dependsOn,
                            ManualActionTypeEnum manualActionType,
                            String inputRefs) {
    }
}
