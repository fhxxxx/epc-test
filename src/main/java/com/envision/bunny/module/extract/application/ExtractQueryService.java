package com.envision.bunny.module.extract.application;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envision.extract.facade.azure.BlobStorageRemote;
import com.envision.extract.infrastructure.easyExcel.AutoColumnWidthStrategy;
import com.envision.extract.infrastructure.mybatis.BasicPagination;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.infrastructure.util.JsonUtils;
import com.envision.extract.infrastructure.util.MsgUtils;
import com.envision.extract.module.extract.application.dtos.*;
import com.envision.extract.module.extract.application.query.ExtractKeywordQuery;
import com.envision.extract.module.extract.domain.*;
import com.envision.extract.module.uploadfile.application.UploadFileCommandService;
import com.envision.extract.module.uploadfile.domain.UploadTypeEnum;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StreamUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:17
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@EnableConfigurationProperties(DefaultExtractConfig.class)
public class ExtractQueryService {
    private final ExtractRunVersionRepository extractRunVersionRepository;
    private final ExtractRunRepository extractRunRepository;
    private final ExtractRunFileRepository extractRunFileRepository;
    private final ExtractTaskRepository extractTaskRepository;
    private final ExtractTaskResultRepository extractTaskResultRepository;
    private final ExtractRunAssembler extractRunAssembler;
    private final BlobStorageRemote blobStorageRemote;
    private final DefaultExtractConfig commonConfig;
    private final UploadFileCommandService uploadFileCommandService;
    private final TaskExecutor taskExecutor;

    public BasicPagination<ExtractRunPageDTO> queryByKeyword(ExtractKeywordQuery query) {
        IPage<ExtractRunVersion> page = extractRunVersionRepository.queryExtractRunWithStatus(new Page<>(query.getPageNum(), query.getPageSize()), query.getProjectId(), query.getStatus());
        //增加文件列表的返回
        List<Long> versionIds = page.getRecords().stream().map(ExtractRunVersion::getId).toList();
        List<ExtractRunFile> extractRunFiles = extractRunFileRepository.queryWithVersionIds(versionIds);
        Map<Long, List<ExtractRunFile>> fileMap = extractRunFiles.stream().collect(Collectors.groupingBy(ExtractRunFile::getExtractRunId));

        return BasicPagination.of(page, extractRunVersion -> extractRunAssembler.toExtractRunPageDTO(extractRunVersion, fileMap));
    }

    public ExtractRunVersionDTO getById(Long projectId, Long extractRunId) {
        ExtractRun extractRun = extractRunRepository.getById(extractRunId);
        if (extractRun == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.not.found.error"));
        }

        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getBy(projectId, extractRunId, extractRun.getCurrentVersion());
        if (extractRunVersion == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("version.not.found.error"));
        }

        List<ExtractRunFile> extractRunFiles = extractRunFileRepository.queryBy(extractRunVersion.getProjectId(), extractRunVersion.getExtractRunId(), extractRunVersion.getVersion());
        List<ExtractTask> extractTasks = extractTaskRepository.queryBy(projectId, extractRunVersion.getExtractRunId(), extractRunVersion.getVersion());

        List<ExtractTaskResult> extractTaskResults = new ArrayList<>();
        if (!CollectionUtils.isEmpty(extractTasks)) {
            List<Long> extractTaskIds = extractTasks.stream().map(ExtractTask::getId).toList();
            extractTaskResults = extractTaskResultRepository.getByTaskIds(extractTaskIds);
        }

        return extractRunAssembler.toExtractRunDTO(extractRunVersion, extractRunFiles, extractTasks, extractTaskResults);

    }

    public JsonNode getOcrResult(Long projectId, Long extractRunId) throws IOException {
        ExtractRun extractRun = extractRunRepository.getById(extractRunId);
        if (extractRun == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.not.found.error"));
        }

        ExtractRunVersion extractRunVersion = extractRunVersionRepository.getBy(projectId, extractRunId, extractRun.getCurrentVersion());
        if (extractRunVersion == null || extractRunVersion.getOcrResult() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("result.not.found.error"));
        }

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            blobStorageRemote.loadStream(extractRunVersion.getOcrResult(), outputStream);
            return JsonUtils.toJsonNode(StreamUtils.copyToString(outputStream, StandardCharsets.UTF_8));
        }
    }

    /**
     * 将提取结果导出至输出流中
     *
     */
    public void exportExtractResult(OutputStream outputStream, Long projectId, Long extractRunId, String companyCode) {
        List<ExtractRunFileDTO> extractRunFileDTOS = this.getExtractResult(projectId, extractRunId, companyCode);
        List<ResultExportDTO> resultList = this.assembleExportResult(extractRunFileDTOS);

        try (ExcelWriter excelWriter = EasyExcelFactory.write(outputStream)
                .registerWriteHandler(new AutoColumnWidthStrategy()).build()) {
            WriteSheet singleSheet = EasyExcelFactory.writerSheet(0, "发票提取结果")
                    .head(ResultExportDTO.class)
                    .build();
            excelWriter.write(resultList, singleSheet);
        }
    }

    private List<ExtractRunFileDTO> getExtractResult(Long projectId, Long extractRunId, String companyCode) {
        ExtractRun extractRun = Optional.ofNullable(extractRunRepository.getById(extractRunId)).orElseThrow(() ->
                new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.run.not.found.error")));
        ExtractRunVersion extractRunVersion = Optional.ofNullable(extractRunVersionRepository.getBy(projectId, extractRunId,
                extractRun.getCurrentVersion())).orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("version.not.found.error")));
        if (extractRunVersion.getStatus() != ExtractRunStatusEnum.SUCCESS) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("extract.result.not.ready.error"));
        }
        List<ExtractRunFile> extractRunFiles = extractRunFileRepository.queryBy(projectId, extractRunVersion.getExtractRunId(), extractRunVersion.getVersion(), companyCode);
        if (CollUtil.isEmpty(extractRunFiles)) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("result.not.found.error"));
        }
        List<ExtractTask> extractTasks = extractTaskRepository.queryBy(projectId, extractRunVersion.getExtractRunId(), extractRunVersion.getVersion(),
                extractRunFiles.stream().map(ExtractRunFile::getFileId).collect(Collectors.toSet()));

        List<ExtractTaskResult> extractTaskResults = new ArrayList<>();
        if (!CollectionUtils.isEmpty(extractTasks)) {
            List<Long> extractTaskIds = extractTasks.stream().map(ExtractTask::getId).toList();
            extractTaskResults = extractTaskResultRepository.getByTaskIds(extractTaskIds);
        }
        return extractRunAssembler.toExtractRunFileDTOs(extractRunFiles, extractTasks, extractTaskResults);
    }

    private List<ResultExportDTO> assembleExportResult(List<ExtractRunFileDTO> extractRunFiles) {
        List<ResultExportDTO> resultList = new ArrayList<>();
        for (ExtractRunFileDTO extractRunFile : extractRunFiles) {
            List<ExtractTaskDTO> extractTasks = extractRunFile.getExtractTasks();
            for (ExtractTaskDTO extractTask : extractTasks) {
                resultList.add(extractRunAssembler.toResultExportDTOList(extractTask.getSingleFieldResults()));
            }
        }
        return resultList;
    }

    public List<ExtractConfigDTO> getConfigList() {
        return extractRunAssembler.toExtractConfigDTOList(commonConfig.getConfigList());
    }

    public void resultUpload(Long projectId, Long extractRunId) {
        //获取所有的结果
        List<ExtractRunFileDTO> extractRunFileDTOS = this.getExtractResult(projectId, extractRunId, null);
        //按照公司代码grouping
        Map<String, List<ExtractRunFileDTO>> companyCodeMap = extractRunFileDTOS.stream().collect(Collectors.groupingBy(ExtractRunFileDTO::getCompanyCode));

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (Map.Entry<String, List<ExtractRunFileDTO>> entry : companyCodeMap.entrySet()) {
            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    List<ResultExportDTO> resultList = this.assembleExportResult(entry.getValue());

                    try (ExcelWriter excelWriter = EasyExcelFactory.write(outputStream)
                            .registerWriteHandler(new AutoColumnWidthStrategy()).build()) {
                        WriteSheet singleSheet = EasyExcelFactory.writerSheet(0, "发票提取结果")
                                .head(ResultExportDTO.class)
                                .build();
                        excelWriter.write(resultList, singleSheet);
                    }

                    String fileName = "发票提取结果" + "-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
                    String path = "extractResult/" + UUID.randomUUID().toString().replace("-", "") + ".xlsx";

                    ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
                    uploadFileCommandService.uploadExcel(inputStream, projectId, entry.getKey(), fileName, path, UploadTypeEnum.RESULT);
                } catch (Exception e) {
                    log.error("公司代码 {} 结果导入异常{}", entry.getKey(), e.getMessage(), e);
                    throw new BizException(ErrorCode.SYS_ERROR, "公司代码 " + entry.getKey() + " 处理失败" + e.getMessage());
                }
            }, taskExecutor);

            futures.add(future);
        }

        // 等待所有任务完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

}
