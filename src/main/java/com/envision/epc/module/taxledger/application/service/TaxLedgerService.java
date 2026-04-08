package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.command.ConfirmStageCommand;
import com.envision.epc.module.taxledger.application.command.CreateLedgerRunCommand;
import com.envision.epc.module.taxledger.application.dto.LedgerRunDetailDTO;
import com.envision.epc.module.taxledger.domain.*;
import com.envision.epc.module.taxledger.excel.TaxLedgerExcelService;
import com.envision.epc.module.taxledger.infrastructure.*;
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
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaxLedgerService {
    private final TaxLedgerRecordMapper ledgerRecordMapper;
    private final TaxLedgerRunMapper ledgerRunMapper;
    private final TaxLedgerRunStageMapper stageMapper;
    private final TaxLedgerRunArtifactMapper artifactMapper;
    private final TaxFileRecordMapper fileRecordMapper;
    private final BlobStorageRemote blobStorageRemote;
    private final TaxLedgerExcelService excelService;
    private final TaxPermissionService permissionService;

    @Transactional(rollbackFor = Exception.class)
    public LedgerRunDetailDTO createRun(CreateLedgerRunCommand command) {
        permissionService.checkCompanyAccess(command.getCompanyCode());
        TaxLedgerRecord ledger = getOrCreateLedgerRecord(command.getCompanyCode(), command.getYearMonth());
        invalidateOldRuns(ledger.getId());

        TaxLedgerRun run = new TaxLedgerRun();
        run.setLedgerId(ledger.getId());
        run.setRunNo(nextRunNo(ledger.getId()));
        run.setTriggerType(LedgerRunTriggerEnum.MANUAL);
        run.setModeSnapshot(command.getMode() == null ? LedgerRunModeEnum.AUTO : command.getMode());
        run.setStatus(LedgerRunStatusEnum.RUNNING);
        run.setCurrentBatch(1);
        run.setInputFingerprint(UUID.randomUUID().toString().replace("-", ""));
        run.setTemplateCode("tax-ledger-v1");
        run.setTemplateVersion("1.0.0");
        run.setTemplateChecksum("N/A");
        run.setStartedAt(LocalDateTime.now());
        run.setIsDeleted(0);
        ledgerRunMapper.insert(run);
        ledger.setLatestRunId(run.getId());
        ledger.setGenerateStatus(LedgerGenerateStatusEnum.PENDING);
        ledger.setStatusMsg("Run started");
        ledgerRecordMapper.updateById(ledger);

        createStages(run.getId());
        executeRun(run);
        return getRunDetail(run.getId());
    }

    public LedgerRunDetailDTO getRunDetail(Long runId) {
        TaxLedgerRun run = ledgerRunMapper.selectById(runId);
        if (run == null || run.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Run not found");
        }
        TaxLedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
        permissionService.checkCompanyAccess(ledger.getCompanyCode());
        List<TaxLedgerRunStage> stages = stageMapper.selectList(new LambdaQueryWrapper<TaxLedgerRunStage>()
                .eq(TaxLedgerRunStage::getIsDeleted, 0)
                .eq(TaxLedgerRunStage::getRunId, runId)
                .orderByAsc(TaxLedgerRunStage::getBatchNo));
        List<TaxLedgerRunArtifact> artifacts = artifactMapper.selectList(new LambdaQueryWrapper<TaxLedgerRunArtifact>()
                .eq(TaxLedgerRunArtifact::getIsDeleted, 0)
                .eq(TaxLedgerRunArtifact::getRunId, runId)
                .orderByAsc(TaxLedgerRunArtifact::getBatchNo));
        return LedgerRunDetailDTO.of(run, stages, artifacts);
    }

    @Transactional(rollbackFor = Exception.class)
    public LedgerRunDetailDTO confirm(Long runId, ConfirmStageCommand command) {
        TaxLedgerRun run = ledgerRunMapper.selectById(runId);
        if (run == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Run not found");
        }
        if (run.getStatus() != LedgerRunStatusEnum.PAUSED) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Run is not paused");
        }
        TaxLedgerRunStage stage = stageMapper.selectOne(new LambdaQueryWrapper<TaxLedgerRunStage>()
                .eq(TaxLedgerRunStage::getIsDeleted, 0)
                .eq(TaxLedgerRunStage::getRunId, runId)
                .eq(TaxLedgerRunStage::getBatchNo, command.getBatchNo()));
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

    public List<TaxLedgerRun> listRuns(String companyCode, String yearMonth) {
        permissionService.checkCompanyAccess(companyCode);
        TaxLedgerRecord ledger = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<TaxLedgerRecord>()
                .eq(TaxLedgerRecord::getIsDeleted, 0)
                .eq(TaxLedgerRecord::getCompanyCode, companyCode)
                .eq(TaxLedgerRecord::getYearMonth, yearMonth));
        if (ledger == null) {
            return Collections.emptyList();
        }
        return ledgerRunMapper.selectList(new LambdaQueryWrapper<TaxLedgerRun>()
                .eq(TaxLedgerRun::getIsDeleted, 0)
                .eq(TaxLedgerRun::getLedgerId, ledger.getId())
                .orderByDesc(TaxLedgerRun::getRunNo));
    }

    public void downloadFinalLedger(String companyCode, String yearMonth, HttpServletResponse response) throws IOException {
        permissionService.checkCompanyAccess(companyCode);
        TaxLedgerRecord ledger = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<TaxLedgerRecord>()
                .eq(TaxLedgerRecord::getIsDeleted, 0)
                .eq(TaxLedgerRecord::getCompanyCode, companyCode)
                .eq(TaxLedgerRecord::getYearMonth, yearMonth));
        if (ledger == null || ledger.getBlobPath() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Final ledger is not available");
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode(ledger.getLedgerName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        blobStorageRemote.loadStream(ledger.getBlobPath(), response.getOutputStream());
    }

    private void executeRun(TaxLedgerRun run) {
        TaxLedgerRecord ledger = ledgerRecordMapper.selectById(run.getLedgerId());
        List<TaxFileRecord> files = fileRecordMapper.selectList(new LambdaQueryWrapper<TaxFileRecord>()
                .eq(TaxFileRecord::getIsDeleted, 0)
                .eq(TaxFileRecord::getCompanyCode, ledger.getCompanyCode())
                .eq(TaxFileRecord::getYearMonth, ledger.getYearMonth()));
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
                saveArtifact(run.getId(), 3, LedgerArtifactTypeEnum.FINAL_LEDGER, ledger.getLedgerName(), blobPath, sha256(finalLedger));
                successStage(run.getId(), 3);
                run.setCurrentBatch(4);
            }

            run.setStatus(LedgerRunStatusEnum.SUCCESS);
            run.setEndedAt(LocalDateTime.now());
            ledgerRunMapper.updateById(run);

            ledger.setGenerateStatus(LedgerGenerateStatusEnum.SUCCESS);
            ledger.setGeneratedAt(LocalDateTime.now());
            ledger.setStatusMsg("Success");
            ledger.setBlobPath(String.format("tax-ledger/%s/%s/final/%s", ledger.getCompanyCode(), ledger.getYearMonth(), ledger.getLedgerName()));
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

    private void runBatch(TaxLedgerRun run, int batchNo, boolean success, String failedMsg) {
        TaxLedgerRunStage stage = getStage(run.getId(), batchNo);
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
        byte[] snapshot = ("run=" + run.getId() + ",batch=" + batchNo + ",time=" + LocalDateTime.now()).getBytes(StandardCharsets.UTF_8);
        String path = String.format("tax-ledger/snapshots/run-%d/batch-%d.txt", run.getId(), batchNo);
        blobStorageRemote.upload(path, new ByteArrayInputStream(snapshot));
        saveArtifact(run.getId(), batchNo, LedgerArtifactTypeEnum.STAGE_SNAPSHOT, "batch-" + batchNo + ".txt", path, sha256(snapshot));
        successStage(run.getId(), batchNo);
    }

    private void successStage(Long runId, int batchNo) {
        TaxLedgerRunStage stage = getStage(runId, batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.SUCCESS);
        stage.setSheetCountTotal(1);
        stage.setSheetCountSuccess(1);
        stage.setEndedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
    }

    private void markPaused(TaxLedgerRun run, int batchNo) {
        TaxLedgerRunStage stage = getStage(run.getId(), batchNo);
        stage.setStatus(LedgerRunStageStatusEnum.PENDING);
        stageMapper.updateById(stage);
        run.setStatus(LedgerRunStatusEnum.PAUSED);
        run.setCurrentBatch(batchNo);
        ledgerRunMapper.updateById(run);
    }

    private void saveArtifact(Long runId, Integer batchNo, LedgerArtifactTypeEnum type, String name, String path, String checksum) {
        TaxLedgerRunArtifact artifact = new TaxLedgerRunArtifact();
        artifact.setRunId(runId);
        artifact.setBatchNo(batchNo);
        artifact.setArtifactType(type);
        artifact.setArtifactName(name);
        artifact.setBlobPath(path);
        artifact.setChecksum(checksum);
        artifact.setIsDeleted(0);
        artifactMapper.insert(artifact);
    }

    private TaxLedgerRunStage getStage(Long runId, int batchNo) {
        TaxLedgerRunStage stage = stageMapper.selectOne(new LambdaQueryWrapper<TaxLedgerRunStage>()
                .eq(TaxLedgerRunStage::getRunId, runId)
                .eq(TaxLedgerRunStage::getBatchNo, batchNo)
                .eq(TaxLedgerRunStage::getIsDeleted, 0));
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

    private TaxLedgerRecord getOrCreateLedgerRecord(String companyCode, String yearMonth) {
        TaxLedgerRecord ledger = ledgerRecordMapper.selectOne(new LambdaQueryWrapper<TaxLedgerRecord>()
                .eq(TaxLedgerRecord::getIsDeleted, 0)
                .eq(TaxLedgerRecord::getCompanyCode, companyCode)
                .eq(TaxLedgerRecord::getYearMonth, yearMonth));
        if (ledger != null) {
            return ledger;
        }
        ledger = new TaxLedgerRecord();
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
        List<TaxLedgerRun> oldRuns = ledgerRunMapper.selectList(new LambdaQueryWrapper<TaxLedgerRun>()
                .eq(TaxLedgerRun::getIsDeleted, 0)
                .eq(TaxLedgerRun::getLedgerId, ledgerId)
                .in(TaxLedgerRun::getStatus, LedgerRunStatusEnum.PENDING, LedgerRunStatusEnum.RUNNING, LedgerRunStatusEnum.PAUSED));
        oldRuns.forEach(item -> {
            item.setStatus(LedgerRunStatusEnum.INVALIDATED);
            item.setEndedAt(LocalDateTime.now());
            ledgerRunMapper.updateById(item);
        });
    }

    private int nextRunNo(Long ledgerId) {
        TaxLedgerRun latest = ledgerRunMapper.selectOne(new LambdaQueryWrapper<TaxLedgerRun>()
                .eq(TaxLedgerRun::getIsDeleted, 0)
                .eq(TaxLedgerRun::getLedgerId, ledgerId)
                .orderByDesc(TaxLedgerRun::getRunNo)
                .last("LIMIT 1"));
        if (latest == null) {
            return 1;
        }
        return latest.getRunNo() + 1;
    }

    private void createStages(Long runId) {
        for (int batchNo = 1; batchNo <= 3; batchNo++) {
            TaxLedgerRunStage stage = new TaxLedgerRunStage();
            stage.setRunId(runId);
            stage.setBatchNo(batchNo);
            stage.setStatus(LedgerRunStageStatusEnum.PENDING);
            stage.setDependsOn(batchNo == 1 ? "" : String.valueOf(batchNo - 1));
            stage.setIsDeleted(0);
            stageMapper.insert(stage);
        }
    }
}
