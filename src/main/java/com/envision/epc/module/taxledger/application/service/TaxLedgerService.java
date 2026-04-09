package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.command.ConfirmStageCommand;
import com.envision.epc.module.taxledger.application.command.CreateLedgerRunCommand;
import com.envision.epc.module.taxledger.application.dto.LedgerRunDetailDTO;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.LedgerArtifactTypeEnum;
import com.envision.epc.module.taxledger.domain.LedgerGenerateStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunModeEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunStageStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRunTriggerEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.domain.LedgerRecord;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.LedgerRunStage;
import com.envision.epc.module.taxledger.excel.TaxLedgerExcelService;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRecordMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunArtifactMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunStageMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 台账运行核心服务
 */
@Service
@RequiredArgsConstructor
public class TaxLedgerService {
    private final LedgerRecordMapper ledgerRecordMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunStageMapper stageMapper;
    private final LedgerRunArtifactMapper artifactMapper;
    private final FileRecordMapper fileRecordMapper;
    private final BlobStorageRemote blobStorageRemote;
    private final TaxLedgerExcelService excelService;
    private final PermissionService permissionService;

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
        executeRun(run);
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

        return LedgerRunDetailDTO.of(run, stages, artifacts);
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

        LedgerRunStage stage = stageMapper.selectOne(new LambdaQueryWrapper<LedgerRunStage>()
                .eq(LedgerRunStage::getIsDeleted, 0)
                .eq(LedgerRunStage::getRunId, runId)
                .eq(LedgerRunStage::getBatchNo, command.getBatchNo()));
        if (stage == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Stage not found");
        }

        stage.setStatus(LedgerRunStageStatusEnum.CONFIRMED);
        stage.setConfirmUser(permissionService.currentUserCode());
        stage.setConfirmTime(LocalDateTime.now());
        stageMapper.updateById(stage);

        run.setStatus(LedgerRunStatusEnum.RUNNING);
        run.setCurrentBatch(command.getBatchNo());
        ledgerRunMapper.updateById(run);

        executeRun(run);
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

    private void executeRun(LedgerRun run) {
        LedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
        List<FileRecord> files = fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, ledger.getCompanyCode())
                .eq(FileRecord::getYearMonth, ledger.getYearMonth()));

        Set<FileCategoryEnum> categories = new HashSet<>();
        files.forEach(item -> categories.add(item.getFileCategory()));

        try {
            if (run.getCurrentBatch() <= 1) {
                runBatch(run, 1, categories.contains(FileCategoryEnum.BS) && categories.contains(FileCategoryEnum.PL),
                        "Missing required BS/PL files");
                run.setCurrentBatch(2);
                ledgerRunMapper.updateById(run);
            }

            if (run.getCurrentBatch() <= 2) {
                if (run.getModeSnapshot() == LedgerRunModeEnum.GATED && run.getStatus() != LedgerRunStatusEnum.PAUSED) {
                    markPaused(run, 2);
                    return;
                }
                runBatch(run, 2, true, null);
                run.setCurrentBatch(3);
                ledgerRunMapper.updateById(run);
            }

            if (run.getCurrentBatch() <= 3) {
                byte[] finalLedger = excelService.buildLedger(ledger.getCompanyCode(), ledger.getYearMonth(), files);
                String blobPath = String.format("tax-ledger/%s/%s/final/%s",
                        ledger.getCompanyCode(), ledger.getYearMonth(), ledger.getLedgerName());
                blobStorageRemote.upload(blobPath, new ByteArrayInputStream(finalLedger));
                saveArtifact(run.getId(), 3, LedgerArtifactTypeEnum.FINAL_LEDGER, ledger.getLedgerName(), blobPath,
                        (long) finalLedger.length, sha256(finalLedger));
                successStage(run.getId(), 3);
                run.setCurrentBatch(4);
            }

            run.setStatus(LedgerRunStatusEnum.SUCCESS);
            run.setEndedAt(LocalDateTime.now());
            ledgerRunMapper.updateById(run);

            ledger.setGenerateStatus(LedgerGenerateStatusEnum.SUCCESS);
            ledger.setGeneratedAt(LocalDateTime.now());
            ledger.setStatusMsg("Success");
            ledger.setBlobPath(String.format("tax-ledger/%s/%s/final/%s",
                    ledger.getCompanyCode(), ledger.getYearMonth(), ledger.getLedgerName()));
            ledgerRecordMapper.updateById(ledger);
        } catch (Exception e) {
            run.setStatus(LedgerRunStatusEnum.FAILED);
            run.setErrorCode("RUN_FAILED");
            run.setErrorMsg(e.getMessage());
            run.setEndedAt(LocalDateTime.now());
            ledgerRunMapper.updateById(run);

            ledger.setGenerateStatus(LedgerGenerateStatusEnum.FAILED);
            ledger.setStatusMsg(e.getMessage());
            ledgerRecordMapper.updateById(ledger);
        }
    }

    private void runBatch(LedgerRun run, int batchNo, boolean success, String failedMsg) {
        LedgerRunStage stage = getStage(run.getId(), batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.RUNNING);
        stage.setStartedAt(LocalDateTime.now());
        stageMapper.updateById(stage);

        if (!success) {
            stage.setStatus(LedgerRunStageStatusEnum.FAILED);
            stage.setErrorMsg(failedMsg);
            stage.setEndedAt(LocalDateTime.now());
            stageMapper.updateById(stage);
            throw new BizException(ErrorCode.BAD_REQUEST, failedMsg);
        }

        byte[] snapshot = ("run=" + run.getId() + ",batch=" + batchNo + ",time=" + LocalDateTime.now())
                .getBytes(StandardCharsets.UTF_8);
        String path = String.format("tax-ledger/snapshots/run-%d/batch-%d.txt", run.getId(), batchNo);
        blobStorageRemote.upload(path, new ByteArrayInputStream(snapshot));
        saveArtifact(run.getId(), batchNo, LedgerArtifactTypeEnum.STAGE_SNAPSHOT, "batch-" + batchNo + ".txt", path,
                (long) snapshot.length, sha256(snapshot));
        successStage(run.getId(), batchNo);
    }

    private void successStage(Long runId, int batchNo) {
        LedgerRunStage stage = getStage(runId, batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.SUCCESS);
        stage.setSheetCountTotal(1);
        stage.setSheetCountSuccess(1);
        stage.setEndedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
    }

    private void markPaused(LedgerRun run, int batchNo) {
        LedgerRunStage stage = getStage(run.getId(), batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.PENDING);
        stageMapper.updateById(stage);

        run.setStatus(LedgerRunStatusEnum.PAUSED);
        run.setCurrentBatch(batchNo);
        ledgerRunMapper.updateById(run);
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
        for (int batchNo = 1; batchNo <= 3; batchNo++) {
            LedgerRunStage stage = new LedgerRunStage();
            stage.setRunId(runId);
            stage.setBatchNo(batchNo);
            stage.setStatus(LedgerRunStageStatusEnum.PENDING);
            stage.setDependsOn(batchNo == 1 ? "" : String.valueOf(batchNo - 1));
            stage.setIsDeleted(0);
            stageMapper.insert(stage);
        }
    }
}
