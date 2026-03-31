package com.envision.bunny.module.extract.web;

import com.envision.extract.facade.aliyun.AliyunRemoteService;
import com.envision.extract.facade.azure.BlobStorageRemote;
import com.envision.extract.infrastructure.idempotent.Idempotent;
import com.envision.extract.infrastructure.log.AvoidLog;
import com.envision.extract.infrastructure.mybatis.BasicPagination;
import com.envision.extract.module.extract.application.ExtractCommandService;
import com.envision.extract.module.extract.application.ExtractQueryService;
import com.envision.extract.module.extract.application.command.ExtractCommand;
import com.envision.extract.module.extract.application.command.ExtractResultInsertCommand;
import com.envision.extract.module.extract.application.command.ExtractResultUpdateCommand;
import com.envision.extract.module.extract.application.dtos.ExtractConfigDTO;
import com.envision.extract.module.extract.application.dtos.ExtractRunPageDTO;
import com.envision.extract.module.extract.application.dtos.ExtractRunVersionDTO;
import com.envision.extract.module.extract.application.dtos.ExtractTaskResultDto;
import com.envision.extract.module.extract.application.query.ExtractKeywordQuery;
import com.envision.extract.module.validation.ValidProject;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 提取任务
 *
 * @author wenjun.gu
 * @since 2025/8/12-16:46
 */
@Validated
@RestController
@RequestMapping("/extract-runs")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ExtractRunController {
    private final ExtractCommandService commandService;
    private final ExtractQueryService queryService;
    private final AliyunRemoteService aliyunRemoteService;
    private final BlobStorageRemote blobStorageRemote;

    /**
     * 创建提取
     *
     * @param command 提取参数
     * @return 提取结果
     */
    @PostMapping
    @Idempotent(prefix = "runExtract", spelKey = "#{#command.assembleRedisKey() + '-' + T(com.envision.extract.infrastructure.security.SecurityUtils).getCurrentUserCode()}", expireSeconds = 5)
    public ExtractRunVersionDTO extract(@RequestBody @Valid ExtractCommand command) {
        return commandService.extract(command);
    }

    /**
     * 分页查询任务
     *
     * @param query 查询条件
     * @return 查询结果
     */
    @AvoidLog
    @GetMapping
    public BasicPagination<ExtractRunPageDTO> queryByKeyword(@Valid ExtractKeywordQuery query) {
        return queryService.queryByKeyword(query);
    }

    /**
     * 根据id查询任务
     *
     * @param extractRunId 记录id
     * @return 查询结果
     */
    @GetMapping("/{extractRunId}")
    public ExtractRunVersionDTO getById(@ValidProject Long projectId, @PathVariable(value = "extractRunId") Long extractRunId) {
        return queryService.getById(projectId, extractRunId);
    }

    /**
     * 根据id删除任务
     *
     * @param extractRunId 记录id
     */
    @DeleteMapping("/{extractRunId}")
    public void removeById(@ValidProject Long projectId, @PathVariable(value = "extractRunId") Long extractRunId) {
        commandService.removeById(projectId, extractRunId);
    }

    /**
     * 查询ocr结果
     *
     * @param projectId    项目id
     * @param extractRunId 任务id
     * @return 结果
     */
    @AvoidLog
    @GetMapping("/{extractRunId}/ocr-result")
    public JsonNode getOcrResult(@ValidProject Long projectId, @PathVariable(value = "extractRunId") Long extractRunId) throws IOException {
        return queryService.getOcrResult(projectId, extractRunId);
    }

    /**
     * 结果导出
     *
     * @param extractRunId 任务id
     */
    @AvoidLog
    @GetMapping("/{extractRunId}/export")
    public void exportExtractResult(@ValidProject Long projectId, @PathVariable(value = "extractRunId") Long extractRunId,
                                    @RequestParam String companyCode, HttpServletResponse response) throws IOException {
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("[" + companyCode + "]提取结果导出", StandardCharsets.UTF_8).replace("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName + "_" + System.currentTimeMillis() + ".xlsx");
        queryService.exportExtractResult(response.getOutputStream(), projectId, extractRunId, companyCode);
    }

    /**
     * 将提取结果生成excel并上传至文件管理中去
     *
     * @param extractRunId 任务id
     */
    @AvoidLog
    @GetMapping("/{extractRunId}/result-upload")
    public void resultUpload(@ValidProject Long projectId, @PathVariable(value = "extractRunId") Long extractRunId) {
        queryService.resultUpload(projectId, extractRunId);
    }

    /**
     * 插入提取结果
     *
     */
    @PostMapping("/result-insert")
    public List<ExtractTaskResultDto> resultInsert(@RequestBody ExtractResultInsertCommand insertCommand) {
        return commandService.insertExtractResult(insertCommand);
    }

    /**
     * 编辑提取结果
     *
     */
    @PostMapping("/result-update")
    public void resultUpdate(@RequestBody @Valid ExtractResultUpdateCommand updateCommand) {
        commandService.updateExtractResult(updateCommand);
    }

    /**
     * 删除提取结果
     *
     */
    @DeleteMapping("/result-delete/{compositeIndex}")
    public void resultDelete(@ValidProject Long projectId, @PathVariable(value = "compositeIndex") Long compositeIndex) {
        commandService.deleteExtractResult(projectId, compositeIndex);
    }

    /**
     * 查询默认的提取配置列表
     *
     */
    @GetMapping("/get/config-list")
    public List<ExtractConfigDTO> getConfigList() {
        return queryService.getConfigList();
    }

    /**
     * 测试阿里云OCR识别
     *
     * @return
     *
     */
    @GetMapping("/test/extract")
    public String getExtract(String url, Integer pageNo) {
        return aliyunRemoteService.extract(url,pageNo);
    }

    /**
     * 测试azure的blobtoken
     *
     * @return
     *
     */
    @GetMapping("/test/fileToken")
    public String getExtract(String fileName) {
        return blobStorageRemote.generateSasUrl(fileName);
    }



}
