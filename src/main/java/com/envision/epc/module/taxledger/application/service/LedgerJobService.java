package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.command.CreateLedgerJobCommand;
import com.envision.epc.module.taxledger.application.command.CreateLedgerRunCommand;
import com.envision.epc.module.taxledger.application.dto.LedgerJobListDTO;
import com.envision.epc.module.taxledger.application.dto.LedgerRunDetailDTO;
import com.envision.epc.module.taxledger.domain.LedgerArtifactTypeEnum;
import com.envision.epc.module.taxledger.domain.LedgerJob;
import com.envision.epc.module.taxledger.domain.LedgerJobStatusEnum;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import com.envision.epc.module.taxledger.domain.LedgerRunArtifact;
import com.envision.epc.module.taxledger.domain.LedgerRunStatusEnum;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.infrastructure.LedgerJobMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunArtifactMapper;
import com.envision.epc.module.taxledger.infrastructure.LedgerRunMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerJobService {
    private static final int MAX_WAIT_SECONDS = 1800;

    private final LedgerJobMapper ledgerJobMapper;
    private final LedgerRunMapper ledgerRunMapper;
    private final LedgerRunArtifactMapper artifactMapper;
    private final LedgerPrecheckService precheckService;
    private final TaxLedgerService ledgerService;
    private final FileService fileService;
    private final PermissionService permissionService;
    private final BlobStorageRemote blobStorageRemote;
    private final TaskExecutor taskExecutor;

    @Transactional(rollbackFor = Exception.class)
    public LedgerJob createJob(CreateLedgerJobCommand command) {
        validateCommand(command);
        permissionService.checkCompanyAccess(command.getCompanyCode());

        LedgerJob running = findRunning(command.getCompanyCode(), command.getYearMonth());
        if (running != null) {
            return running;
        }

        LedgerJob job = new LedgerJob();
        job.setCompanyCode(command.getCompanyCode());
        job.setYearMonth(command.getYearMonth());
        job.setStatus(LedgerJobStatusEnum.PENDING);
        job.setIsDeleted(0);
        ledgerJobMapper.insert(job);
        dispatchJobAfterCommit(job.getId());
        return job;
    }

    public LedgerJobListDTO list(String companyCode, String yearMonth, int page, int size) {
        LambdaQueryWrapper<LedgerJob> query = new LambdaQueryWrapper<LedgerJob>()
                .eq(LedgerJob::getIsDeleted, 0)
                .eq(StringUtils.hasText(companyCode), LedgerJob::getCompanyCode, companyCode)
                .eq(StringUtils.hasText(yearMonth), LedgerJob::getYearMonth, yearMonth)
                .orderByDesc(LedgerJob::getCreateTime);

        if (!StringUtils.hasText(companyCode) && !permissionService.canAccessAllCompanies()) {
            List<String> granted = permissionService.listGrantedCompanyCodes();
            if (granted.isEmpty()) {
                return LedgerJobListDTO.of(List.of(), 0L, page, size);
            }
            query.in(LedgerJob::getCompanyCode, granted);
        } else if (StringUtils.hasText(companyCode)) {
            permissionService.checkCompanyAccess(companyCode);
        }

        Page<LedgerJob> pager = ledgerJobMapper.selectPage(new Page<>(page, size), query);
        return LedgerJobListDTO.of(pager.getRecords(), pager.getTotal(), page, size);
    }

    public LedgerJob detail(Long jobId) {
        LedgerJob job = getJob(jobId);
        permissionService.checkCompanyAccess(job.getCompanyCode());
        return job;
    }

    @Transactional(rollbackFor = Exception.class)
    public LedgerJob retry(Long jobId) {
        LedgerJob current = getJob(jobId);
        permissionService.checkCompanyAccess(current.getCompanyCode());
        if (current.getStatus() != LedgerJobStatusEnum.FAILED && current.getStatus() != LedgerJobStatusEnum.VALIDATION_FAILED) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Only failed jobs can be retried");
        }
        CreateLedgerJobCommand command = new CreateLedgerJobCommand();
        command.setCompanyCode(current.getCompanyCode());
        command.setYearMonth(current.getYearMonth());
        return createJob(command);
    }

    public void download(Long jobId, HttpServletResponse response) throws IOException {
        LedgerJob job = getJob(jobId);
        permissionService.checkCompanyAccess(job.getCompanyCode());
        if (job.getStatus() != LedgerJobStatusEnum.SUCCESS || job.getRunId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Job is not successful yet");
        }

        LedgerRunArtifact artifact = artifactMapper.selectOne(new LambdaQueryWrapper<LedgerRunArtifact>()
                .eq(LedgerRunArtifact::getIsDeleted, 0)
                .eq(LedgerRunArtifact::getRunId, job.getRunId())
                .eq(LedgerRunArtifact::getArtifactType, LedgerArtifactTypeEnum.FINAL_LEDGER)
                .orderByDesc(LedgerRunArtifact::getId)
                .last("LIMIT 1"));
        if (artifact == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Final ledger artifact not found");
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode(artifact.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        blobStorageRemote.loadStream(artifact.getBlobPath(), response.getOutputStream());
    }

    public FileRecord uploadFinalLedger(String companyCode, String yearMonth, MultipartFile file) throws IOException {
        return fileService.upload(companyCode, yearMonth, FileCategoryEnum.FINAL_LEDGER, file);
    }

    @Transactional(rollbackFor = Exception.class)
    public FileRecord publishFinalLedger(Long jobId) {
        LedgerJob job = getJob(jobId);
        permissionService.checkCompanyAccess(job.getCompanyCode());
        if (job.getStatus() != LedgerJobStatusEnum.SUCCESS || job.getRunId() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "仅成功任务可设为最终台账");
        }

        LedgerRunArtifact artifact = artifactMapper.selectOne(new LambdaQueryWrapper<LedgerRunArtifact>()
                .eq(LedgerRunArtifact::getIsDeleted, 0)
                .eq(LedgerRunArtifact::getRunId, job.getRunId())
                .eq(LedgerRunArtifact::getArtifactType, LedgerArtifactTypeEnum.FINAL_LEDGER)
                .orderByDesc(LedgerRunArtifact::getId)
                .last("LIMIT 1"));
        if (artifact == null || !StringUtils.hasText(artifact.getBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "任务未找到最终台账产物");
        }

        Long fileSize = artifact.getFileSize() == null ? 0L : artifact.getFileSize();
        return fileService.saveOrReplace(
                job.getCompanyCode(),
                job.getYearMonth(),
                artifact.getFileName(),
                FileCategoryEnum.FINAL_LEDGER,
                artifact.getBlobPath(),
                fileSize
        );
    }

    private LedgerJob getJob(Long jobId) {
        LedgerJob job = ledgerJobMapper.selectById(jobId);
        if (job == null || job.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "Job not found");
        }
        return job;
    }

    private LedgerJob findRunning(String companyCode, String yearMonth) {
        return ledgerJobMapper.selectOne(new LambdaQueryWrapper<LedgerJob>()
                .eq(LedgerJob::getIsDeleted, 0)
                .eq(LedgerJob::getCompanyCode, companyCode)
                .eq(LedgerJob::getYearMonth, yearMonth)
                .in(LedgerJob::getStatus, LedgerJobStatusEnum.VALIDATING, LedgerJobStatusEnum.GENERATING)
                .orderByDesc(LedgerJob::getCreateTime)
                .last("LIMIT 1"));
    }

    private void validateCommand(CreateLedgerJobCommand command) {
        if (command == null || !StringUtils.hasText(command.getCompanyCode()) || !StringUtils.hasText(command.getYearMonth())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "companyCode/yearMonth are required");
        }
    }

    private void dispatchJobAfterCommit(Long jobId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dispatchJob(jobId);
                }
            });
            return;
        }
        dispatchJob(jobId);
    }

    private void dispatchJob(Long jobId) {
        taskExecutor.execute(() -> processJob(jobId));
    }

    private void processJob(Long jobId) {
        try {
            LedgerJob job = getJob(jobId);
            job.setStatus(LedgerJobStatusEnum.VALIDATING);
            job.setStartedAt(LocalDateTime.now());
            job.setErrorMsg(null);
            ledgerJobMapper.updateById(job);

            try {
                precheckService.precheck(job.getCompanyCode(), job.getYearMonth());
            } catch (BizException ex) {
                fail(job, LedgerJobStatusEnum.VALIDATION_FAILED, ex.getMessage());
                return;
            }

            job.setStatus(LedgerJobStatusEnum.GENERATING);
            ledgerJobMapper.updateById(job);

            CreateLedgerRunCommand runCommand = new CreateLedgerRunCommand();
            runCommand.setCompanyCode(job.getCompanyCode());
            runCommand.setYearMonth(job.getYearMonth());
            LedgerRunDetailDTO detail = ledgerService.createRun(runCommand);
            job.setRunId(detail.getRunId());
            ledgerJobMapper.updateById(job);

            waitRunTerminal(job);
        } catch (BizException ex) {
            log.warn("ledger job failed, jobId={}, msg={}", jobId, ex.getMessage());
            safeFail(jobId, ex.getMessage());
        } catch (Exception ex) {
            log.error("ledger job failed, jobId={}", jobId, ex);
            safeFail(jobId, ex.getMessage());
        }
    }

    private void waitRunTerminal(LedgerJob job) throws InterruptedException {
        int waited = 0;
        while (waited < MAX_WAIT_SECONDS) {
            LedgerRun run = ledgerRunMapper.selectById(job.getRunId());
            if (run == null || run.getIsDeleted() == 1) {
                fail(job, LedgerJobStatusEnum.FAILED, "Run not found");
                return;
            }
            if (run.getStatus() == LedgerRunStatusEnum.SUCCESS) {
                job.setStatus(LedgerJobStatusEnum.SUCCESS);
                job.setEndedAt(LocalDateTime.now());
                job.setErrorMsg(null);
                ledgerJobMapper.updateById(job);
                return;
            }
            if (run.getStatus() == LedgerRunStatusEnum.FAILED
                    || run.getStatus() == LedgerRunStatusEnum.CANCELED
                    || run.getStatus() == LedgerRunStatusEnum.INVALIDATED
                    || run.getStatus() == LedgerRunStatusEnum.PAUSED) {
                String msg = StringUtils.hasText(run.getErrorMsg()) ? run.getErrorMsg() : "Generation failed";
                fail(job, LedgerJobStatusEnum.FAILED, msg);
                return;
            }
            Thread.sleep(1000L);
            waited++;
        }
        fail(job, LedgerJobStatusEnum.FAILED, "Job timeout");
    }

    private void safeFail(Long jobId, String errorMsg) {
        LedgerJob job = ledgerJobMapper.selectById(jobId);
        if (job == null || job.getIsDeleted() == 1) {
            return;
        }
        fail(job, LedgerJobStatusEnum.FAILED, errorMsg);
    }

    private void fail(LedgerJob job, LedgerJobStatusEnum status, String errorMsg) {
        job.setStatus(status);
        job.setErrorMsg(StringUtils.hasText(errorMsg) ? errorMsg : "Job failed");
        job.setEndedAt(LocalDateTime.now());
        ledgerJobMapper.updateById(job);
    }
}
