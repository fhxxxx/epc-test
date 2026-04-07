package com.envision.epc.module.uploadfile.web;

import cn.hutool.core.text.CharSequenceUtil;
import com.envision.epc.infrastructure.log.AvoidLog;
import com.envision.epc.infrastructure.mybatis.BasicPagination;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.infrastructure.util.MsgUtils;
import com.envision.epc.module.extract.application.validations.CompareValidation;
import com.envision.epc.module.uploadfile.application.UploadFileCommandService;
import com.envision.epc.module.uploadfile.application.UploadFileQueryService;
import com.envision.epc.module.uploadfile.application.dtos.UploadFileDTO;
import com.envision.epc.module.uploadfile.application.query.KeywordQuery;
import com.envision.epc.module.uploadfile.domain.UploadTypeEnum;
import com.envision.epc.module.validation.ValidProject;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 文件
 *
 * @author chaoyue.zhao
 * @since 2025-08-07
 */
@RestController
@Validated
@RequestMapping("/files")
@EnableConfigurationProperties(CompareValidation.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UploadFileController {
    private final UploadFileCommandService uploadFileCommandService;
    private final UploadFileQueryService uploadFileQueryService;
    private final CompareValidation compareValidation;

    /**
     * 上传文件
     *
     * @param file      文件
     * @param projectId 项目ID
     * @return 上传结果
     */
    @PostMapping(value = "/upload")
    public UploadFileDTO uploadFiles(@RequestPart MultipartFile file, @RequestParam @ValidProject Long projectId,
                                     @RequestParam String companyCode) throws IOException{
        if (companyCode.length() != compareValidation.getCompanyCodeLimit()) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("company.code.limit.error", compareValidation.getCompanyCodeLimit()));
        }
        return uploadFileCommandService.uploadFile(file, projectId, companyCode);
    }

    /**
     * 批量删除文件
     *
     * @param fileIds 文件ID列表
     */
    @DeleteMapping
    public void deleteFiles(@RequestParam("ids") List<Long> fileIds) {
        uploadFileCommandService.deleteFiles(fileIds);
    }

    /**
     * 文件预览
     *
     * @param fileId   文件ID
     * @param response 响应体
     */
    @GetMapping("/{fileId}/file-preview")
    public void filePreview(@PathVariable("fileId") Long fileId, HttpServletResponse response, HttpServletRequest request) {
        uploadFileQueryService.filePreview(fileId, response, request);
    }

    /**
     * 分页查询文件列表
     *
     * @param projectId    项目ID
     * @param keywordQuery 查询参数
     * @return 文件列表
     */
    @GetMapping("/{projectId}")
    public BasicPagination<UploadFileDTO> queryFiles(@PathVariable("projectId") @ValidProject Long projectId, KeywordQuery keywordQuery) {
        return uploadFileQueryService.queryFiles(projectId, keywordQuery);
    }

    /**
     * 查询文件列表
     *
     * @param projectId    项目ID
     * @param keywordQuery 查询参数
     * @return 文件列表
     */
    @GetMapping("/queryAllFiles/{projectId}")
    public List<UploadFileDTO> queryAllFiles(@PathVariable("projectId") @ValidProject Long projectId, KeywordQuery keywordQuery) {
        return uploadFileQueryService.queryAllFiles(projectId, keywordQuery);
    }


    /**
     * 上传excel类型文件
     *
     * @param file      文件
     * @param projectId 项目ID
     * @return 上传结果
     */
    @PostMapping(value = "/upload-excel")
    public UploadFileDTO uploadExcel(@RequestPart MultipartFile file, @RequestParam @ValidProject Long projectId,
                                     @NotNull UploadTypeEnum type, @RequestParam String companyCode) throws IOException {
        if (Objects.isNull(file) || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件为空");
        }
        String contentType = file.getContentType();
        if (StringUtils.isEmpty(contentType)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "contentType 为空");
        }
        String filename = file.getOriginalFilename();
        if (StringUtils.isEmpty(filename) || !filename.contains(".")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件名称为空");
        }
        if (!filename.endsWith(".xlsx") && !filename.endsWith(".xls")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件格式错误,只能选择.xlsx或.xls类型文件");
        }
        if (companyCode.length() != compareValidation.getCompanyCodeLimit()) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("company.code.limit.error", compareValidation.getCompanyCodeLimit()));
        }
        String path = "";
        if (type == UploadTypeEnum.TAXBUREAU) {
            path += "taxBureau/";
        } else if (type == UploadTypeEnum.RESULT) {
            path += "extractResult/";
        } else if (type == UploadTypeEnum.DATALAKECOMPARE) {
            path += "dataLakeCompare/";
        }
        if (CharSequenceUtil.isBlank(path)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件类型错误,只能选择TAXBUREAU,RESULT,DATALAKECOMPARE类型文件");
        }
        path += UUID.randomUUID().toString().replace("-", "") + ".xlsx";
        return uploadFileCommandService.uploadExcel(file.getInputStream(), projectId, companyCode, filename, path, type);
    }

    /**
     * excel结果导出
     *
     */
    @AvoidLog
    @GetMapping("/{fileId}/export-excel")
    public void exportExcel(@ValidProject Long projectId, @PathVariable(value = "fileId") Long fileId,
                            HttpServletResponse response) throws IOException {
        uploadFileCommandService.exportExcel(projectId, fileId, response);
    }

}
