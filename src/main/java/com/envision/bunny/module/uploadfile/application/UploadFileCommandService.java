package com.envision.bunny.module.uploadfile.application;

import com.aspose.pdf.Document;
import com.envision.extract.facade.azure.BlobStorageRemote;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.infrastructure.util.ApplicationContextUtils;
import com.envision.extract.infrastructure.util.AsposeUtils;
import com.envision.extract.infrastructure.util.MsgUtils;
import com.envision.extract.module.event.ProjectDeleteEvent;
import com.envision.extract.module.extract.application.validations.UploadFileValidation;
import com.envision.extract.module.uploadfile.application.dtos.UploadFileDTO;
import com.envision.extract.module.uploadfile.domain.UploadFile;
import com.envision.extract.module.uploadfile.domain.UploadFileRepository;
import com.envision.extract.module.uploadfile.domain.UploadTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * @author wenjun.gu
 * @since 2025/8/14-11:33
 */
@Slf4j
@Service
@EnableConfigurationProperties(UploadFileValidation.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UploadFileCommandService {
    public static final List<String> PDF_EXTENSIONS = List.of(".pdf");
    public static final List<String> WORD_EXTENSIONS = List.of(".doc", ".docx");
    public static final List<String> IMAGE_EXTENSIONS = List.of(".jpg", ".png", ".jpeg");
    private final UploadFileAssembler uploadFileAssembler;
    private final BlobStorageRemote blobStorageRemote;
    private final UploadFileRepository uploadFileRepository;
    private final UploadFileValidation uploadFileValidation;

    public UploadFileDTO uploadFile(MultipartFile file, Long projectId, String companyCode) throws IOException {
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
        String extension = getFileExtension(filename).toLowerCase();
        if (!PDF_EXTENSIONS.contains(extension) && !WORD_EXTENSIONS.contains(extension) && !IMAGE_EXTENSIONS.contains(extension)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件格式错误");
        }

        String pdfFilename = UUID.randomUUID().toString().replace("-", "") + ".pdf";
        String path = "extract/" + pdfFilename;
        Path tempFilePath = Files.createTempFile("upload_", extension);
        Path pdfTempPath = null;

        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            try (InputStream in = file.getInputStream();
                 OutputStream out = Files.newOutputStream(tempFilePath)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                    md.update(buffer, 0, len);
                }
            }
            String hash = HexFormat.of().formatHex(md.digest());

            if (WORD_EXTENSIONS.contains(extension)) {
                pdfTempPath = Files.createTempFile("converted_", ".pdf");
                AsposeUtils.wordToPdf(pdfTempPath.toString(), Files.newInputStream(tempFilePath));
            } else if (IMAGE_EXTENSIONS.contains(extension)) {
                pdfTempPath = Files.createTempFile("converted_", ".pdf");
                AsposeUtils.imageToPdf(pdfTempPath.toString(), Files.newInputStream(tempFilePath));
            } else {
                pdfTempPath = tempFilePath;
            }

            int pages;
            try (InputStream pdfStream = Files.newInputStream(pdfTempPath)) {
                Document document = AsposeUtils.loadPDF(pdfStream);
                log.info("Document hash: [{}] isLinearized: [{}]", hash, document.isLinearized());
                pages = document.getPages().size();
            }

            if (pages > uploadFileValidation.getMaxPageCount()) {
                throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("file.max.page.error", uploadFileValidation.getMaxPageCount()));
            }

            long size;
            try (InputStream uploadStream = Files.newInputStream(pdfTempPath)) {
                size = uploadStream.available();

                if (size > uploadFileValidation.getMaxSize() * 1024 * 1024) {
                    throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("file.max.size.error", uploadFileValidation.getMaxSize()));
                }
                blobStorageRemote.upload(path, uploadStream);
            }

            // 保存记录
            UploadFile uploadFile = new UploadFile(projectId, companyCode, filename, pages, size, path, hash, UploadTypeEnum.INVOICE);
            uploadFileRepository.save(uploadFile);

            return uploadFileAssembler.toUploadFileDTO(uploadFile);
        } catch (NoSuchAlgorithmException e) {
            throw new BizException(ErrorCode.SYS_ERROR);
        } finally {
            Files.deleteIfExists(tempFilePath);
            if (pdfTempPath != null) {
                Files.deleteIfExists(pdfTempPath);
            }
        }
    }

    private String getFileExtension(String filename) {
        int index = filename.lastIndexOf(".");
        return index != -1 ? filename.substring(index).toLowerCase() : "";
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteFiles(List<Long> fileIds) {
        if (fileIds.isEmpty()) {
            return;
        }
        List<UploadFile> uploadFiles = uploadFileRepository.listByIds(fileIds);
        for (UploadFile uploadFile : uploadFiles) {
            if (uploadFile.getType() == UploadTypeEnum.DATALAKECOMPARE) {
                continue;
            }
            blobStorageRemote.delete(uploadFile.getPath());
        }
        uploadFileRepository.lambdaUpdate()
                .in(UploadFile::getId, fileIds)
                .remove();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByProjectId(ProjectDeleteEvent event) {
        Long projectId = event.getProjectId();
        uploadFileRepository.lambdaUpdate()
                .eq(UploadFile::getProjectId, projectId)
                .remove();
    }

    public UploadFileDTO uploadExcel(InputStream inputStream, Long projectId, String companyCode, String fileName, String filePath, UploadTypeEnum type) throws IOException {
        long size = inputStream.available();
        blobStorageRemote.upload(filePath, inputStream);
        UploadFileCommandService uploadFileCommandService = ApplicationContextUtils.getBean(UploadFileCommandService.class);

        return uploadFileCommandService.uploadExcel(projectId, companyCode, fileName, filePath, type, size);
    }

    @Transactional
    public UploadFileDTO uploadExcel(Long projectId, String companyCode, String fileName, String filePath, UploadTypeEnum type, Long fileSize) {
        UploadFile uploadFile = new UploadFile(projectId, companyCode, fileName, 1, fileSize, filePath, "", type);
        uploadFileRepository.save(uploadFile);
        return uploadFileAssembler.toUploadFileDTO(uploadFile);
    }

    @Transactional
    public UploadFileDTO uploadExcel(Long projectId, String companyCode, String fileName, String filePath, UploadTypeEnum type,
                                     Long fileSize, String createBy, String createByName, LocalDateTime createTime) {
        UploadFile uploadFile = new UploadFile(projectId, companyCode, fileName, 1, fileSize, filePath, "", type,
                createBy, createByName, createTime);
        uploadFileRepository.save(uploadFile);
        return uploadFileAssembler.toUploadFileDTO(uploadFile);
    }

    /**
     * excel类型文件导出
     */
    public void exportExcel(Long projectId, Long fileId, HttpServletResponse response) throws IOException {
        UploadFile file = uploadFileRepository.lambdaQuery()
                .eq(UploadFile::getProjectId, projectId)
                .eq(UploadFile::getId, fileId).one();

        if (file == null || !blobStorageRemote.exists(file.getPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("file.not.found.error"));
        }

        if (!UploadTypeEnum.EXCEL_FILE_LIST.contains(file.getType())) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("file.type.not.support.error"));
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("[" + file.getCompanyCode() + "]" + file.getName(), StandardCharsets.UTF_8).replace("\\+", "%20");
        response.setHeader("Content-Disposition", "attachment;filename=" + fileName);
        blobStorageRemote.loadStream(file.getPath(), response.getOutputStream());
    }
}
