package com.envision.bunny.module.extract.application;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.aspose.pdf.Document;
import com.envision.extract.facade.aliyun.AliyunRemoteService;
import com.envision.extract.facade.azure.BlobStorageRemote;
import com.envision.extract.facade.azure.InvoiceRemote;
import com.envision.extract.facade.dify.DifyRemoteService;
import com.envision.extract.infrastructure.mybatis.MyIdGenerator;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.infrastructure.util.AsposeUtils;
import com.envision.extract.infrastructure.util.MsgUtils;
import com.envision.extract.module.event.ProjectDeleteEvent;
import com.envision.extract.module.extract.application.command.ExtractCommand;
import com.envision.extract.module.extract.application.command.ExtractResultInsertCommand;
import com.envision.extract.module.extract.application.command.ExtractResultUpdateCommand;
import com.envision.extract.module.extract.application.dtos.ExtractRunVersionDTO;
import com.envision.extract.module.extract.application.dtos.ExtractTaskResultDto;
import com.envision.extract.module.extract.application.exceptions.ExtractRunException;
import com.envision.extract.module.extract.application.tasks.Constants;
import com.envision.extract.module.extract.application.tasks.MessageProducer;
import com.envision.extract.module.extract.application.validations.ExtractValidation;
import com.envision.extract.module.extract.domain.*;
import com.envision.extract.module.extract.domain.configs.ExtractConfigFactory;
import com.envision.extract.module.uploadfile.domain.UploadFile;
import com.envision.extract.module.uploadfile.domain.UploadFileRepository;
import com.envision.extract.module.uploadfile.domain.UploadTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * @author wenjun.gu
 * @since 2025/8/12-19:28
 */
@Slf4j
@Service
@SuppressWarnings("unchecked")
@EnableConfigurationProperties({ExtractValidation.class, DefaultExtractConfig.class})
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ExtractCommandService {
    private final ExtractRunAssembler extractRunAssembler;
    private final ExtractRunRepository extractRunRepository;
    private final ExtractRunVersionRepository extractRunVersionRepository;
    private final ExtractRunFileRepository extractRunFileRepository;
    private final OcrTaskRepository ocrTaskRepository;
    private final ExtractTaskRepository extractTaskRepository;
    private final UploadFileRepository uploadFileRepository;
    private final MessageProducer messageProducer;
    private final BlobStorageRemote blobStorageRemote;
    private final InvoiceRemote invoiceRemote;
    private final AliyunRemoteService aliyunRemoteService;
    private final ApplicationContext applicationContext;
    private final ExtractValidation extractValidation;
    private final ExtractTaskResultRepository extractTaskResultRepository;
    private final MyIdGenerator myIdGenerator;
    private final DifyRemoteService difyRemoteService;
    private final DefaultExtractConfig commonConfig;

    private final String ALIYUN_MODEL_NAME = "aliyun";


    /**
     * 执行发票提取流程的入口方法
     * 
     * @param command 提取命令参数对象，包含项目ID、文件IDs、提取配置等信息
     * @return ExtractRunVersionDTO 提取运行版本数据传输对象
     */
    public ExtractRunVersionDTO extract(ExtractCommand command) {
        //使用系统配置，接口入参不再支持
        ExtractConfig extractConfig = commonConfig.getExtractConfig(command.getExtractConfig().getId());
        if (extractConfig == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "no such config");
        }
        command.setExtractConfig(extractConfig);

        ExtractCommandService bean = applicationContext.getBean(ExtractCommandService.class);
        if (command.getExtractRunId() == null) {
            return bean.createExtract(command);
        } else {
            return bean.updateExtract(command);
        }
    }

    /**
     * 创建新的提取任务
     * 
     * @param command 提取命令参数对象
     * @return ExtractRunVersionDTO 提取运行版本数据传输对象
     */
    @Transactional(rollbackFor = Exception.class)
    public ExtractRunVersionDTO createExtract(ExtractCommand command) {
        if (command.getFileIds().isEmpty() || command.getFileIds().size() > extractValidation.getMaxFileCount()) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.file.count.limit.error", extractValidation.getMaxFileCount()));
        }
        List<UploadFile> uploadFiles = uploadFileRepository.lambdaQuery()
                .eq(UploadFile::getProjectId, command.getProjectId())
                .in(UploadFile::getId, command.getFileIds())
                .list();
        if (uploadFiles.size() != command.getFileIds().size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.file.not.found.error"));
        }
        if (uploadFiles.stream().anyMatch(file -> file.getType() != UploadTypeEnum.INVOICE)) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.file.type.error"));
        }
        if (uploadFiles.stream().anyMatch(file -> CharSequenceUtil.isBlank(file.getCompanyCode()))) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("file.company.code.not.found"));
        }

        int totalPages = uploadFiles.stream().reduce(0, (sum, file) -> sum + file.getPages(), Integer::sum);
        if (totalPages > extractValidation.getMaxFilePages()) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.page.count.limit.error", extractValidation.getMaxFilePages()));
        }

        ExtractRun extractRun = extractRunAssembler.fromExtractCommand(command, 1);
        ExtractRunVersion extractRunVersion = extractRunAssembler.fromExtractCommand(command, 1, ExtractRunStatusEnum.PENDING);

        List<ExtractRunFile> extractRunFiles = new ArrayList<>();
        for (UploadFile uploadFile : uploadFiles) {
            extractRunFiles.add(extractRunAssembler.buildExtractRunVersionFile(command.getProjectId(), extractRun.getCurrentVersion()
                    , uploadFile.getCompanyCode(), uploadFile.getHash(), uploadFile.getId(), uploadFile.getName(), ExtractRunStatusEnum.PENDING));
        }

        extractRunRepository.save(extractRun);
        extractRunVersion.setExtractRunId(extractRun.getId());
        extractRunVersionRepository.save(extractRunVersion);
        extractRunFiles.forEach(extractRunFile -> extractRunFile.setExtractRunId(extractRun.getId()));
        extractRunFileRepository.saveBatch(extractRunFiles, 500);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageProducer.sendMessage(Constants.OCR_TASK, Map.of(Constants.EXTRACT_RUN_KEY, String.valueOf(extractRunVersion.getId())));
            }
        });
        return extractRunAssembler.toExtractRunDTO(extractRunVersion, extractRunFiles);
    }

    /**
     * 更新已有的提取任务
     * 
     * @param command 提取命令参数对象
     * @return ExtractRunVersionDTO 提取运行版本数据传输对象
     */
    @Transactional(rollbackFor = Exception.class)
    public ExtractRunVersionDTO updateExtract(ExtractCommand command) {
        ExtractRun extractRun = extractRunRepository.getById(command.getExtractRunId());
        if (extractRun == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.not.found.error"));
        }

        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getBy(extractRun.getProjectId(), extractRun.getId(), extractRun.getCurrentVersion());
        if (extractRunVersion == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("version.not.found.error"));
        }
        if (!Arrays.asList(ExtractRunStatusEnum.SUCCESS, ExtractRunStatusEnum.OCR_FAILED, ExtractRunStatusEnum.EXTRACT_FAILED).contains(extractRunVersion.getStatus())) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("run.update.error", extractRunVersion.getStatus()));
        }

        List<UploadFile> uploadFiles = uploadFileRepository.lambdaQuery()
                .eq(UploadFile::getProjectId, command.getProjectId())
                .in(UploadFile::getId, command.getFileIds())
                .list();
        if (uploadFiles.size() != command.getFileIds().size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("file.not.found.error"));
        }
        if (uploadFiles.stream().anyMatch(file -> file.getType() != UploadTypeEnum.INVOICE)) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.file.type.error"));
        }
        if (uploadFiles.stream().anyMatch(file -> CharSequenceUtil.isBlank(file.getCompanyCode()))) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("file.company.code.not.found"));
        }

        Integer currentVersion = extractRun.getCurrentVersion();
        Integer newVersion = currentVersion + 1;
        extractRun.setCurrentVersion(newVersion);
        ExtractRunVersion newExtractRunVersion = extractRunAssembler.fromExtractCommand(command, extractRun.getId(), newVersion, ExtractRunStatusEnum.PENDING);

        List<ExtractRunFile> extractRunFiles = new ArrayList<>();
        for (UploadFile uploadFile : uploadFiles) {
            extractRunFiles.add(extractRunAssembler.buildExtractRunVersionFile(command.getProjectId(), extractRun.getId()
                    , newVersion, uploadFile.getCompanyCode(), uploadFile.getHash(), uploadFile.getId(), uploadFile.getName(), ExtractRunStatusEnum.PENDING));
        }

        boolean update = extractRunRepository.lambdaUpdate()
                .eq(ExtractRun::getId, extractRun.getId())
                .eq(ExtractRun::getCurrentVersion, currentVersion)
                .set(ExtractRun::getCurrentVersion, newVersion)
                .update();
        if (!update) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.updating.error"));
        }
        extractRunVersionRepository.save(newExtractRunVersion);
        extractRunFileRepository.saveBatch(extractRunFiles, 500);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                messageProducer.sendMessage(Constants.OCR_TASK, Map.of(Constants.EXTRACT_RUN_KEY, String.valueOf(newExtractRunVersion.getId())));
            }
        });
        return extractRunAssembler.toExtractRunDTO(newExtractRunVersion, extractRunFiles);
    }

    /**
     * 提交OCR任务，开始对文件进行光学字符识别
     * 
     * @param extractRunVersionId 提取运行版本ID
     */
    public void submitOcr(Long extractRunVersionId) {
        log.info("Submit ocr started: [{}]", extractRunVersionId);
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getById(extractRunVersionId);
        if (extractRunVersion == null || extractRunVersion.getStatus() != ExtractRunStatusEnum.PENDING) {
            log.info("ExtractRunVersion not found or not pending: [{}] [{}]", extractRunVersionId, extractRunVersion != null ? extractRunVersion.getStatus() : "");
            return;
        }
        extractRunVersion.ocrRunning();
        extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus);
        List<ExtractRunFile> extractRunFiles = extractRunFileRepository.queryBy(extractRunVersion.getProjectId(), extractRunVersion.getExtractRunId(), extractRunVersion.getVersion());
        extractRunFiles.forEach(ExtractRunFile::ocrRunning);
        extractRunFileRepository.updateBatchById(extractRunFiles, ExtractRunFile::getStatus);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        try {
            for (ExtractRunFile extractRunFile : extractRunFiles) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> submitExtractRunFile(extractRunFile), executorService);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
//            messageProducer.sendDelayMessage(Map.of(Constants.EXTRACT_RUN_KEY, String.valueOf(extractRunVersion.getId())), 5);
            //直接跳到提取
            messageProducer.sendMessage(Constants.EXTRACT_TASK, Map.of(Constants.EXTRACT_RUN_KEY, String.valueOf(extractRunVersion.getId())));
            log.info("Submit ocr finished: [{}]", extractRunVersionId);
        } catch (Exception e) {
            extractRunVersion.ocrFailed(ExceptionUtils.getStackTrace(e));
            extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus, ExtractRunVersion::getError);
            log.error("Submit Ocr Task error: [{}]", extractRunVersionId, e);
        }
    }

    /**
     * 提交单个文件的OCR任务
     * 
     * @param extractRunFile 提取运行文件对象
     */
    public void submitExtractRunFile(ExtractRunFile extractRunFile) {
        try {
            UploadFile uploadFile = uploadFileRepository.getById(extractRunFile.getFileId());
            if (uploadFile == null) {
                throw new ExtractRunException(MsgUtils.getMessage("fileId.not.found.error", extractRunFile.getFileId()));
            }
            List<int[]> pageRanges = generatePageRanges(uploadFile.getPages(), 1);
            List<OcrTask> ocrTasks = new ArrayList<>();
            for (int i = 0; i < pageRanges.size(); i++) {
                OcrTask ocrTask = extractRunAssembler.buildOcrTask(extractRunFile.getProjectId(), extractRunFile.getExtractRunId(),
                        extractRunFile.getVersion(), extractRunFile.getFileId(), uploadFile.getHash(), i + 1,
                        pageRanges.get(i)[0], pageRanges.get(i)[1], OcrTaskStatusEnum.PENDING, extractRunFile.getCreateBy(),
                        extractRunFile.getCreateByName(), LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
                ocrTasks.add(ocrTask);
            }

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                blobStorageRemote.loadStream(uploadFile.getPath(), outputStream);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                Document document = AsposeUtils.loadPDF(inputStream);

                for (OcrTask ocrTask : ocrTasks) {
                    String resultPath = String.format("result/%s-%s_%s-%s.json", ALIYUN_MODEL_NAME, uploadFile.getHash(), ocrTask.getStartPage(), ocrTask.getEndPage());
                    if (blobStorageRemote.exists(resultPath)) {
                        log.info("Submit ocr task found exist result: [{}] [{}]-[{}]", extractRunFile.getId(), ocrTask.getStartPage(), ocrTask.getEndPage());
                        ocrTask.success(resultPath);
                        continue;
                    }

                    try (ByteArrayOutputStream newPdfOutputStream = new ByteArrayOutputStream()) {
                        //将PDF按照页数切成单页文件流
                        AsposeUtils.extractPdfRange(document, newPdfOutputStream, ocrTask.getStartPage(), ocrTask.getEndPage());

                        String result = aliyunRemoteService.extract(new ByteArrayInputStream(newPdfOutputStream.toByteArray()));
                        ocrTask.success(resultPath);
                        blobStorageRemote.upload(resultPath, new ByteArrayInputStream(result.getBytes(StandardCharsets.UTF_8)));
                        log.info("Submit ocr task success: [{}] [{}]-[{}]", ocrTask.getFileId(), ocrTask.getStartPage(), ocrTask.getEndPage());
                        //防止限流,每次调用完接口等待1s
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            throw new BizException(ErrorCode.SYS_ERROR);
                        }
                    }
                }
            }
            ocrTaskRepository.saveBatch(ocrTasks, 500);
        } catch (IOException e) {
            extractRunFile.ocrFailed(ExceptionUtils.getStackTrace(e));
            extractRunFileRepository.updateById(extractRunFile, ExtractRunFile::getStatus, ExtractRunFile::getError);
            log.error("Submit extract run file IOException error: [{}]", extractRunFile.getId(), e);
            throw new BizException(ErrorCode.SYS_ERROR);
        } catch (Exception e) {
            extractRunFile.ocrFailed(ExceptionUtils.getStackTrace(e));
            extractRunFileRepository.updateById(extractRunFile, ExtractRunFile::getStatus, ExtractRunFile::getError);
            log.error("Submit extract run file error: [{}]", extractRunFile.getId(), e);
            throw e;
        }
    }

    /**
     * 监听OCR任务执行状态
     * 
     * @param extractRunVersionId 提取运行版本ID
     */
    public void listenOcr(Long extractRunVersionId) {
        log.info("Listen ocr started: [{}]", extractRunVersionId);
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getById(extractRunVersionId);
        if (extractRunVersion == null || extractRunVersion.getStatus() != ExtractRunStatusEnum.OCR_RUNNING) {
            log.info("ExtractRunVersion not found or not running: [{}] [{}]", extractRunVersionId, extractRunVersion != null ? extractRunVersion.getStatus() : "");
            return;
        }

        List<ExtractRunFile> extractRunFiles = extractRunFileRepository.queryBy(extractRunVersion.getProjectId(), extractRunVersion.getExtractRunId(), extractRunVersion.getVersion());
        ExecutorService executorService = Executors.newFixedThreadPool(5);

        try {
            AtomicBoolean allComplete = new AtomicBoolean(true);
            for (ExtractRunFile extractRunFile : extractRunFiles) {
                listenExtractRunFile(extractRunVersion, extractRunFile, allComplete, executorService);
            }
            if (allComplete.get()) {
                String ocrResult = mergeOcrData(extractRunVersion, extractRunFiles);
                extractRunVersion.ocrSuccess(ocrResult);
                extractRunFiles.forEach(ExtractRunFile::ocrSuccess);
                extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus, ExtractRunVersion::getOcrResult);
                messageProducer.sendMessage(Constants.EXTRACT_TASK, Map.of(Constants.EXTRACT_RUN_KEY, String.valueOf(extractRunVersion.getId())));
                log.info("Listen ocr success: [{}]", extractRunVersionId);
            } else {
                messageProducer.sendDelayMessage(Map.of(Constants.EXTRACT_RUN_KEY, String.valueOf(extractRunVersion.getId())), 5);
                log.info("Listen ocr continue: [{}]", extractRunVersionId);
            }
        } catch (Exception e) {
            extractRunVersion.ocrFailed(ExceptionUtils.getStackTrace(e));
            extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus, ExtractRunVersion::getError);
            log.error("Listen ocr failed: [{}]", extractRunVersion.getId(), e);
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * 监听单个提取运行文件的OCR任务
     * 
     * @param extractRunVersion 提取运行版本对象
     * @param extractRunFile 提取运行文件对象
     * @param allComplete 是否全部完成的原子布尔值
     * @param executorService 线程池执行器
     */
    public void listenExtractRunFile(ExtractRunVersion extractRunVersion, ExtractRunFile extractRunFile, AtomicBoolean allComplete, ExecutorService executorService) {
        try {
            AtomicBoolean fileComplete = new AtomicBoolean(true);
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            List<OcrTask> ocrTasks = ocrTaskRepository.queryBy(extractRunVersion.getProjectId(), extractRunVersion.getExtractRunId(), extractRunVersion.getVersion(), extractRunFile.getFileId(), OcrTaskStatusEnum.RUNNING);
            for (OcrTask ocrTask : ocrTasks) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> listenOcrTask(ocrTask, allComplete, fileComplete, extractRunFile.getHash()), executorService);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            if (fileComplete.get()) {
                extractRunFile.ocrSuccess();
                extractRunFileRepository.updateById(extractRunFile, ExtractRunFile::getStatus);
            }
        } catch (Exception e) {
            extractRunFile.ocrFailed(ExceptionUtils.getStackTrace(e));
            extractRunFileRepository.updateById(extractRunFile, ExtractRunFile::getStatus, ExtractRunFile::getError);
            log.error("Listen extractRunFile error: [{}]", extractRunFile.getId(), e);
            throw e;
        }
    }

    /**
     * 监听单个OCR任务的执行状态
     * 
     * @param ocrTask OCR任务对象
     * @param allComplete 是否全部完成的原子布尔值
     * @param fileComplete 当前文件是否完成的原子布尔值
     * @param fileHash 文件哈希值
     */
    public void listenOcrTask(OcrTask ocrTask, AtomicBoolean allComplete, AtomicBoolean fileComplete, String fileHash) {
        try {
            JsonNode result = invoiceRemote.getResult(ocrTask.getRequestId());
            String status = result.get("status").asText();
            if ("succeeded".equals(status)) {
                String resultPath = String.format("result/%s_%s-%s.json", fileHash, ocrTask.getStartPage(), ocrTask.getEndPage());
                ocrTask.success(resultPath);
                blobStorageRemote.upload(resultPath, new ByteArrayInputStream(result.toString().getBytes(StandardCharsets.UTF_8)));
                ocrTaskRepository.updateById(ocrTask, OcrTask::getStatus, OcrTask::getResult);
            } else if ("running".equals(status)) {
                allComplete.set(false);
                fileComplete.set(false);
            } else {
                String text = result.has("message") ? result.get("message").asText() : "Unknown error";
                throw new BizException(ErrorCode.REMOTE_ERROR, text);
            }
        } catch (Exception e) {
            ocrTask.failed(ExceptionUtils.getStackTrace(e));
            ocrTaskRepository.updateById(ocrTask, OcrTask::getStatus, OcrTask::getError);
            log.error("Listen ocrTask error: [{}]", ocrTask.getId(), e);
            throw e;
        }
    }

    /**
     * 执行提取任务，从OCR结果中提取结构化数据
     * 
     * @param extractRunVersionId 提取运行版本ID
     */
    public void extract(Long extractRunVersionId) {
        log.info("Extract started: [{}]", extractRunVersionId);
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getById(extractRunVersionId);

        if (extractRunVersion == null) {
            log.info("ExtractRunVersion not found: [{}]", extractRunVersionId);
            return;
        }
        extractRunVersion.extractRunning();
        extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus);

        ExecutorService executorService = Executors.newFixedThreadPool(5);
        List<ExtractRunFile> extractRunFiles = extractRunFileRepository.queryBy(extractRunVersion.getProjectId(), extractRunVersion.getExtractRunId(), extractRunVersion.getVersion());

        try {
            for (ExtractRunFile extractRunFile : extractRunFiles) {
                extractExtractRunFile(extractRunVersion, extractRunFile, executorService);
            }
            extractRunVersion.extractSuccess();
            extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus);
        } catch (Exception e) {
            extractRunVersion.extractFailed(ExceptionUtils.getStackTrace(e));
            extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus, ExtractRunVersion::getError);
            log.error("Extract error: [{}]", extractRunVersion.getId(), e);
        } finally {
            executorService.shutdown();
        }
    }

    /**
     * 对单个提取运行文件执行提取操作
     * 
     * @param extractRunVersion 提取运行版本对象
     * @param extractRunFile 提取运行文件对象
     * @param executorService 线程池执行器
     */
    public void extractExtractRunFile(ExtractRunVersion extractRunVersion, ExtractRunFile extractRunFile, ExecutorService executorService) {
        try {
            List<OcrTask> ocrTasks = ocrTaskRepository.queryBy(extractRunFile.getProjectId(), extractRunFile.getExtractRunId()
                    , extractRunFile.getVersion(), extractRunFile.getFileId());
            List<CompletableFuture<ExtractTask>> futures = new ArrayList<>();
            for (OcrTask ocrTask : ocrTasks) {
                CompletableFuture<ExtractTask> future = CompletableFuture.supplyAsync(
                        () -> subExtract(ocrTask, extractRunVersion.getExtractConfig()), executorService);
                futures.add(future);
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            List<ExtractTask> extractTasks = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
            extractRunFile.extractSuccess();
            extractTaskRepository.saveBatch(extractTasks);
            List<ExtractTaskResult> extractTaskResults = new ArrayList<>();
            extractTasks.forEach(extractTask -> extractTask.getExtractTaskResults().forEach(extractTaskResult -> {
                extractTaskResults.add(extractRunAssembler.assembleExtractTaskResult(extractTaskResult, extractRunVersion.getProjectId(),
                        extractTask.getExtractRunId(), extractTask.getId(), extractTask.getVersion(), extractTask.getCreateBy(),
                        extractTask.getCreateByName(), extractTask.getCreateTime()));
            }));
            extractTaskResultRepository.saveBatch(extractTaskResults, 100);
            extractRunFileRepository.updateById(extractRunFile, ExtractRunFile::getStatus);
        } catch (Exception e) {
            extractRunFile.extractFailed(ExceptionUtils.getStackTrace(e));
            extractRunFileRepository.updateById(extractRunFile, ExtractRunFile::getStatus, ExtractRunFile::getError);
            log.error("Extract ExtractRunFile failed [{}]", extractRunFile.getId(), e);
            throw e;
        }
    }

    /**
     * 子提取任务，处理单个OCR任务的结果
     * 
     * @param ocrTask OCR任务对象
     * @param config 提取配置
     * @return ExtractTask 提取任务对象
     */
    public ExtractTask subExtract(OcrTask ocrTask, ExtractConfig config) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ExtractTask extractTask = extractRunAssembler.buildExtractResult(ocrTask.getProjectId(), ocrTask.getExtractRunId(),
                    ocrTask.getVersion(), ocrTask.getFileId(), ocrTask.getPosition(), ocrTask.getStartPage(),
                    ocrTask.getEndPage(), ocrTask.getCreateBy(), ocrTask.getCreateByName(), LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            blobStorageRemote.loadStream(ocrTask.getResult(), outputStream);
            String markdownResult = StreamUtils.copyToString(outputStream, StandardCharsets.UTF_8);

            ExtractConfig extractConfig = ExtractConfigFactory.getExtractConfig(config);
            List<ExtractTaskResult> taskResults = extractConfig.extract(ocrTask, markdownResult, difyRemoteService, myIdGenerator);
            extractTask.setExtractTaskResults(taskResults);
            return extractTask;
        } catch (Exception e) {
            log.error("Sub extract failed [{}]", ocrTask.getId(), e);
            throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 合并OCR数据
     * 
     * @param extractRunVersion 提取运行版本对象
     * @param extractRunFiles 提取运行文件列表
     * @return String 合并后的OCR结果文件路径
     * @throws IOException IO异常
     */
    private String mergeOcrData(ExtractRunVersion extractRunVersion, List<ExtractRunFile> extractRunFiles) throws IOException {
        log.info("Merge ocr data started: [{}]", extractRunVersion.getId());
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        for (ExtractRunFile extractRunFile : extractRunFiles) {
            List<OcrTask> ocrTasks = ocrTaskRepository.queryBy(extractRunFile.getProjectId(), extractRunFile.getExtractRunId(), extractRunFile.getVersion(), extractRunFile.getFileId());
            if (!ocrTasks.stream().allMatch(ocrTask -> ocrTask.getStatus() == OcrTaskStatusEnum.SUCCESS)) {
                throw new ExtractRunException(String.format("Not all ocr task success: %s", extractRunFile.getId()));
            }

            ObjectNode result = mapper.createObjectNode();
            StringBuilder finalContent = new StringBuilder();
            ArrayNode finalPages = mapper.createArrayNode();
            ArrayNode finalParagraphs = mapper.createArrayNode();
            ArrayNode finalSections = mapper.createArrayNode();
            ArrayNode finalFigures = mapper.createArrayNode();
            ArrayNode finalTables = mapper.createArrayNode();

            int wordOffsetAdjustment = 0;
            int paragraphOffsetAdjustment = 0;
            int sectionOffsetAdjustment = 0;
            int figureOffsetAdjustment = 0;
            int tableOffsetAdjustment = 0;

            for (OcrTask ocrTask : ocrTasks) {

                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                blobStorageRemote.loadStream(ocrTask.getResult(), outputStream);
                String s = StreamUtils.copyToString(outputStream, StandardCharsets.UTF_8);
                outputStream.close();

                ObjectNode node = (ObjectNode) mapper.readTree(s);
                if (node.has("analyzeResult")) {
                    JsonNode analyzeResult = node.get("analyzeResult");
                    JsonNode contentNode = analyzeResult.get("content");

                    Map<Integer, double[]> pageSizeMap = new HashMap<>();
                    // 处理 pages
                    JsonNode pages = analyzeResult.get("pages");
                    if (pages != null) {
                        for (JsonNode page : pages) {
                            int pageNumber = page.get("pageNumber").asInt();
                            pageSizeMap.put(pageNumber, new double[]{page.get("width").asDouble(), page.get("height").asDouble()});
                            ObjectNode newPage = page.deepCopy();
                            // 调整 spans 中的 offset
                            JsonNode spans = newPage.get("spans");
                            if (spans != null) {
                                for (JsonNode span : spans) {
                                    ObjectNode spanObj = (ObjectNode) span;
                                    int originalOffset = spanObj.get("offset").asInt();
                                    spanObj.put("offset", originalOffset + wordOffsetAdjustment);
                                }
                            }

                            // 调整 words 中的 span offset
                            JsonNode words = newPage.get("words");
                            if (words != null) {
                                for (JsonNode word : words) {
                                    ObjectNode wordObj = (ObjectNode) word;
                                    JsonNode span = wordObj.get("span");
                                    if (span != null) {
                                        ObjectNode spanObj = (ObjectNode) span;
                                        int originalOffset = spanObj.get("offset").asInt();
                                        spanObj.put("offset", originalOffset + wordOffsetAdjustment);
                                    }
                                }
                            }

                            // 调整 lines 中的 spans offset
                            //lines 定制化，取到paragraphs中
                            JsonNode lines = newPage.get("lines");
                            if (lines != null) {
                                for (JsonNode line : lines) {
                                    ObjectNode lineObj = (ObjectNode) line;
                                    JsonNode spansInLine = lineObj.get("spans");
                                    if (spansInLine != null) {
                                        for (JsonNode span : spansInLine) {
                                            ObjectNode spanObj = (ObjectNode) span;
                                            int originalOffset = spanObj.get("offset").asInt();
                                            spanObj.put("offset", originalOffset + wordOffsetAdjustment);
                                        }
                                    }
                                    ArrayNode boundingRegions = mapper.createArrayNode();
                                    ObjectNode boundingRegion = mapper.createObjectNode();
                                    boundingRegion.put("height", page.get("height").asDouble());
                                    boundingRegion.put("width", page.get("width").asDouble());
                                    boundingRegion.put("pageNumber", page.get("pageNumber").asInt());
                                    boundingRegion.set("polygon", Optional.ofNullable(lineObj.get("polygon")).orElse(mapper.createArrayNode()));
                                    boundingRegions.add(boundingRegion);
                                    lineObj.set("boundingRegions", boundingRegions);
                                    lineObj.remove("polygon");
                                    finalParagraphs.add(lineObj);
                                }
                            }

                            finalPages.add(newPage);
                        }

                    }

                    // 处理 paragraphs
                    JsonNode paragraphs = analyzeResult.get("paragraphs");
//                    if (paragraphs != null) {
//                        for (JsonNode paragraph : paragraphs) {
//                            ObjectNode newParagraph = paragraph.deepCopy();
//                            JsonNode boundingRegions = newParagraph.get("boundingRegions");
//                            for (JsonNode boundingRegion : boundingRegions) {
//                                ObjectNode boundingRegionObj = (ObjectNode) boundingRegion;
//                                int pageNumber = boundingRegionObj.get("pageNumber").asInt();
//                                double[] doubles = pageSizeMap.get(pageNumber);
//                                if (doubles != null) {
//                                    boundingRegionObj.put("width", doubles[0]);
//                                    boundingRegionObj.put("height", doubles[1]);
//
//                                }
//                            }
//                            JsonNode spans = newParagraph.get("spans");
//                            if (spans != null) {
//                                for (JsonNode span : spans) {
//                                    ObjectNode spanObj = (ObjectNode) span;
//                                    int originalOffset = spanObj.get("offset").asInt();
//                                    spanObj.put("offset", originalOffset + wordOffsetAdjustment);
//                                }
//                            }
//                            finalParagraphs.add(newParagraph);
//                        }
//                    }

                    // 处理 sessions
                    JsonNode sections = analyzeResult.get("sections");
                    if (sections != null) {
                        for (JsonNode section : sections) {
                            ObjectNode newSection = section.deepCopy();
                            JsonNode spans = newSection.get("spans");
                            if (spans != null) {
                                for (JsonNode span : spans) {
                                    ObjectNode spanObj = (ObjectNode) span;
                                    int originalOffset = spanObj.get("offset").asInt();
                                    spanObj.put("offset", originalOffset + wordOffsetAdjustment);
                                }
                            }
                            JsonNode elements = newSection.get("elements");
                            if (elements != null) {
                                ArrayNode newElements = mapper.createArrayNode();
                                for (JsonNode element : elements) {
                                    newElements.add(updateElement(element.asText(), paragraphOffsetAdjustment, sectionOffsetAdjustment, figureOffsetAdjustment, tableOffsetAdjustment));
                                }
                                newSection.set("elements", newElements);

                            }
                            finalSections.add(newSection);
                        }
                    }

                    // 处理 figures
                    JsonNode figures = analyzeResult.get("figures");
                    if (figures != null) {
                        for (JsonNode figure : figures) {
                            ObjectNode newFigure = figure.deepCopy();
                            JsonNode spans = newFigure.get("spans");
                            if (spans != null) {
                                for (JsonNode span : spans) {
                                    ObjectNode spanObj = (ObjectNode) span;
                                    int originalOffset = spanObj.get("offset").asInt();
                                    spanObj.put("offset", originalOffset + wordOffsetAdjustment);
                                }
                            }
                            JsonNode elements = newFigure.get("elements");
                            if (elements != null) {
                                ArrayNode newElements = mapper.createArrayNode();
                                for (JsonNode element : elements) {
                                    newElements.add(updateElement(element.asText(), paragraphOffsetAdjustment, sectionOffsetAdjustment, figureOffsetAdjustment, tableOffsetAdjustment));
                                }
                                newFigure.set("elements", newElements);

                            }
                            finalFigures.add(newFigure);
                        }
                    }

                    // 处理 tables
                    JsonNode tables = analyzeResult.get("tables");
                    if (tables != null) {
                        for (JsonNode table : tables) {
                            ObjectNode newTable = table.deepCopy();
                            // 调整 table spans
                            JsonNode spans = newTable.get("spans");
                            if (spans != null) {
                                for (JsonNode span : spans) {
                                    ObjectNode spanObj = (ObjectNode) span;
                                    int originalOffset = spanObj.get("offset").asInt();
                                    spanObj.put("offset", originalOffset + wordOffsetAdjustment);
                                }
                            }

                            // 调整 cells 中的 spans
                            JsonNode cells = newTable.get("cells");
                            if (cells != null) {
                                for (JsonNode cell : cells) {
                                    ObjectNode cellObj = (ObjectNode) cell;
                                    JsonNode cellSpans = cellObj.get("spans");
                                    if (cellSpans != null) {
                                        for (JsonNode span : cellSpans) {
                                            ObjectNode spanObj = (ObjectNode) span;
                                            int originalOffset = spanObj.get("offset").asInt();
                                            spanObj.put("offset", originalOffset + wordOffsetAdjustment);
                                        }
                                    }
                                    JsonNode elements = cellObj.get("elements");
                                    if (elements != null) {
                                        ArrayNode newElements = mapper.createArrayNode();
                                        for (JsonNode element : elements) {
                                            newElements.add(updateElement(element.asText(), paragraphOffsetAdjustment, sectionOffsetAdjustment, figureOffsetAdjustment, tableOffsetAdjustment));
                                        }
                                        cellObj.set("elements", newElements);
                                    }
                                }
                            }
                            finalTables.add(newTable);
                        }
                    }

                    // 更新 content
                    if (contentNode != null) {
                        String content = contentNode.asText();
                        finalContent.append(content);
                        wordOffsetAdjustment += content.length();
                    }
                    if (paragraphs != null) {
                        paragraphOffsetAdjustment += paragraphs.size();
                    }
                    if (sections != null) {
                        sectionOffsetAdjustment += sections.size();
                    }
                    if (figures != null) {
                        figureOffsetAdjustment += figures.size();
                    }
                    if (tables != null) {
                        tableOffsetAdjustment += tables.size();
                    }
                }
            }

            // 设置最终的 content
//            result.put("content", finalContent.toString());
//            result.set("pages", finalPages);
            result.set("paragraphs", finalParagraphs);
//            result.set("sections", finalSections);
//            result.set("figures", finalFigures);
            result.set("tables", finalTables);
            result.put("fileId", extractRunFile.getFileId());
            result.put("fileName", extractRunFile.getFileName());
            arrayNode.add(result);
        }
        String filename = String.format("ocr_result/%s_%s.json", extractRunVersion.getExtractRunId(), extractRunVersion.getVersion());
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(arrayNode.toString().getBytes(StandardCharsets.UTF_8))) {
            blobStorageRemote.upload(filename, inputStream);
        }
        return filename;
    }

    /**
     * 更新元素引用的偏移量
     * 
     * @param text 元素文本
     * @param paragraphOffsetAdjustment 段落偏移调整值
     * @param sectionOffsetAdjustment 章节偏移调整值
     * @param figureOffsetAdjustment 图片偏移调整值
     * @param tableOffsetAdjustment 表格偏移调整值
     * @return String 更新后的元素文本
     */
    private String updateElement(String text, int paragraphOffsetAdjustment, int sectionOffsetAdjustment, int figureOffsetAdjustment, int tableOffsetAdjustment) {
        if (text.startsWith("/paragraphs/")) {
            int i = Integer.parseInt(text.substring("/paragraphs/".length())) + paragraphOffsetAdjustment;
            return "/paragraphs/" + i;
        } else if (text.startsWith("/sections/")) {
            int i = Integer.parseInt(text.substring("/sections/".length())) + sectionOffsetAdjustment;
            return "/sections/" + i;
        } else if (text.startsWith("/figures/")) {
            int i = Integer.parseInt(text.substring("/figures/".length())) + figureOffsetAdjustment;
            return "/figures/" + i;
        } else if (text.startsWith("/tables/")) {
            int i = Integer.parseInt(text.substring("/tables/".length())) + tableOffsetAdjustment;
            return "/tables/" + i;
        } else {
            return text;
        }
    }

    /**
     * 生成页面范围列表
     * 
     * @param totalPages 总页数
     * @param pagesPerExtraction 每次提取的页数
     * @return List<int[]> 页面范围列表
     */
    private List<int[]> generatePageRanges(int totalPages, int pagesPerExtraction) {
        List<int[]> pageRanges = new ArrayList<>();
        int startPage = 1;

        while (startPage <= totalPages) {
            int endPage = Math.min(startPage + pagesPerExtraction - 1, totalPages);
            pageRanges.add(new int[]{startPage, endPage});
            startPage += pagesPerExtraction;
        }

        return pageRanges;
    }

    /**
     * 根据项目ID删除相关数据
     * 
     * @param event 项目删除事件
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteByProjectId(ProjectDeleteEvent event) {
        Long projectId = event.getProjectId();
        extractRunRepository.lambdaUpdate()
                .eq(ExtractRun::getProjectId, projectId)
                .remove();
        extractRunVersionRepository.lambdaUpdate()
                .eq(ExtractRunVersion::getProjectId, projectId)
                .remove();
        extractRunFileRepository.lambdaUpdate()
                .eq(ExtractRunFile::getProjectId, projectId)
                .remove();
        ocrTaskRepository.lambdaUpdate()
                .eq(OcrTask::getProjectId, projectId)
                .remove();
        extractTaskRepository.lambdaUpdate()
                .eq(ExtractTask::getProjectId, projectId)
                .remove();
    }

    /**
     * 根据提取运行ID删除相关数据
     * 
     * @param projectId 项目ID
     * @param extractRunId 提取运行ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeById(Long projectId, Long extractRunId) {
        ExtractRun extractRun = extractRunRepository.getById(extractRunId);
        if (extractRun == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.not.found.error"));
        }
        extractRunRepository.removeById(extractRun);
        extractRunVersionRepository.lambdaUpdate()
                .eq(ExtractRunVersion::getProjectId, projectId)
                .eq(ExtractRunVersion::getExtractRunId, extractRunId)
                .remove();
        extractRunFileRepository.lambdaUpdate()
                .eq(ExtractRunFile::getProjectId, projectId)
                .eq(ExtractRunFile::getExtractRunId, extractRunId)
                .remove();
        ocrTaskRepository.lambdaUpdate()
                .eq(OcrTask::getProjectId, projectId)
                .eq(OcrTask::getExtractRunId, extractRunId)
                .remove();
        extractTaskRepository.lambdaUpdate()
                .eq(ExtractTask::getProjectId, projectId)
                .eq(ExtractTask::getExtractRunId, extractRunId)
                .remove();
    }


    /**
     * 插入提取结果
     * 
     * @param insertCommand 插入命令参数对象
     * @return List<ExtractTaskResultDto> 提取任务结果DTO列表
     */
    @Transactional(rollbackFor = Exception.class)
    public List<ExtractTaskResultDto> insertExtractResult(ExtractResultInsertCommand insertCommand) {
        ExtractRun extractRun = extractRunRepository.getById(insertCommand.getExtractRunId());
        if (extractRun == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.not.found.error"));
        }
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getBy(insertCommand.getProjectId(), extractRun.getId(), extractRun.getCurrentVersion());
        if (extractRunVersion == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("version.not.found.error"));
        }
        ExtractTask extractTask = extractTaskRepository.queryById(insertCommand.getExtractTaskId(), insertCommand.getProjectId(), extractRun.getId(), extractRunVersion.getVersion());
        if (extractTask == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.result.not.found.error"));
        }

        //校验config配置
        List<ParameterConfig> parameterConfigs = extractRunVersion.getExtractConfig().getParameterConfigs();
        List<ParameterConfig> compositeList = parameterConfigs.stream().filter(config -> config.getType()
                == ParameterTypeEnum.COMPOSITE && config.getName().equals(insertCommand.getCompositeName())).toList();
        if (CollectionUtils.isEmpty(compositeList)) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("composite.name.not.found.error"));
        }
        List<PrimitiveConfig> primitiveConfigs = compositeList.get(0).getPrimitiveConfigs();
        if (!CollUtil.isEqualList(primitiveConfigs.stream().map(PrimitiveConfig::getName).sorted().toList(),
                insertCommand.getPrimitiveFields().stream().map(ExtractResultInsertCommand.Field::getPrimitiveName).sorted().toList())) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("composite.config.incorrect.error"));
        }
        Number indexId = myIdGenerator.nextId(null);

        //新增
        Map<String, String> insertFieldMap = insertCommand.getPrimitiveFields().stream().collect(Collectors.toMap(ExtractResultInsertCommand.Field::getPrimitiveName
                , ExtractResultInsertCommand.Field::getContent, (x1, x2) -> x1));
        List<ExtractTaskResult> extractTaskResults = primitiveConfigs.stream().map(config ->
                extractRunAssembler.buildExtractTaskResult(insertCommand.getProjectId(), insertCommand.getExtractRunId(),
                        insertCommand.getExtractTaskId(), extractRunVersion.getVersion(), ParameterTypeEnum.COMPOSITE,
                        insertCommand.getCompositeName(), indexId.longValue(), config.getName(),
                        insertFieldMap.getOrDefault(config.getName(), ""))
        ).toList();
        extractTaskResultRepository.saveBatch(extractTaskResults);

        return extractRunAssembler.buildExtractTaskResultDtoList(extractTaskResults);
    }

    /**
     * 更新提取结果
     * 
     * @param updateCommand 更新命令参数对象
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateExtractResult(ExtractResultUpdateCommand updateCommand) {
        ExtractRun extractRun = extractRunRepository.getById(updateCommand.getExtractRunId());
        if (extractRun == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.not.found.error"));
        }
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getBy(updateCommand.getProjectId(), extractRun.getId(), extractRun.getCurrentVersion());
        if (extractRunVersion == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("version.not.found.error"));
        }
        ExtractTaskResult extractTaskResult = extractTaskResultRepository.getBy(updateCommand.getExtractResultId()
                , updateCommand.getProjectId(), extractRun.getId(), extractRunVersion.getVersion());
        if (extractTaskResult == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.result.not.found.error"));
        }

        extractTaskResult.setContent(updateCommand.getContent());
        extractTaskResultRepository.updateById(extractTaskResult);
    }

    /**
     * 设计上只允许删除组合字段的列，如果索引值传入0则表示单个字段，需要挡住
     * 
     * @param projectId 项目ID
     * @param compositeIndex 组合索引
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteExtractResult(Long projectId, Long compositeIndex) {
        if (compositeIndex == 0) {
            return;
        }
        extractTaskResultRepository.removeByCompositeIndex(projectId, compositeIndex);
    }

    /**
     * 处理超时的OCR任务
     * 
     * @param extractRunVersionId 提取运行版本ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void overTimeOcrTask(Long extractRunVersionId) {
        log.warn("Ocr Task overTime: [{}]", extractRunVersionId);
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getById(extractRunVersionId);
        if (extractRunVersion == null || (extractRunVersion.getStatus() != ExtractRunStatusEnum.PENDING &&
                extractRunVersion.getStatus() != ExtractRunStatusEnum.OCR_RUNNING)) {
            log.info("Ocr Task overTime break ,ExtractRunVersion not found or status not pending/running: [{}] [{}]",
                    extractRunVersionId, extractRunVersion != null ? extractRunVersion.getStatus() : "");
            return;
        }
        extractRunVersion.ocrFailed("ocr task overTime");
        extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus, ExtractRunVersion::getError);
    }

    /**
     * 处理超时的OCR监听任务
     * 
     * @param extractRunVersionId 提取运行版本ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void overTimeOcrListeningTask(Long extractRunVersionId) {
        log.warn("Ocr listening Task overTime: [{}]", extractRunVersionId);
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getById(extractRunVersionId);
        if (extractRunVersion == null || (extractRunVersion.getStatus() != ExtractRunStatusEnum.PENDING &&
                extractRunVersion.getStatus() != ExtractRunStatusEnum.OCR_RUNNING)) {
            log.info("Ocr listening Task overTime break ,ExtractRunVersion not found or status not pending/running: [{}] [{}]",
                    extractRunVersionId, extractRunVersion != null ? extractRunVersion.getStatus() : "");
            return;
        }
        extractRunVersion.ocrFailed("ocr listening task overTime");
        extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus, ExtractRunVersion::getError);

        extractRunFileRepository.updateStatusBatch(extractRunVersion.getExtractRunId(), extractRunVersion.getVersion(),
                ExtractRunStatusEnum.OCR_FAILED, "ocr listening task overTime");

        ocrTaskRepository.updateStatusBatch(extractRunVersion.getExtractRunId(), extractRunVersion.getVersion(),
                OcrTaskStatusEnum.FAILED, "ocr listening task overTime");
    }

    /**
     * 处理超时的提取任务
     * 
     * @param extractRunVersionId 提取运行版本ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void overTimeExtractTask(Long extractRunVersionId) {
        log.warn("Ocr extract Task overTime: [{}]", extractRunVersionId);
        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getById(extractRunVersionId);
        if (extractRunVersion == null || (extractRunVersion.getStatus() != ExtractRunStatusEnum.EXTRACT_RUNNING &&
                extractRunVersion.getStatus() != ExtractRunStatusEnum.OCR_SUCCESS)) {
            log.info("Ocr extract Task overTime break ,ExtractRunVersion not found or status not correct: [{}] [{}]",
                    extractRunVersionId, extractRunVersion != null ? extractRunVersion.getStatus() : "");
            return;
        }
        extractRunVersion.extractFailed("ocr extract task overTime");
        extractRunVersionRepository.updateById(extractRunVersion, ExtractRunVersion::getStatus, ExtractRunVersion::getError);

        extractRunFileRepository.updateStatusBatch(extractRunVersion.getExtractRunId(), extractRunVersion.getVersion(),
                ExtractRunStatusEnum.EXTRACT_FAILED, "ocr extract task overTime");
    }

}
