package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

/**
 * 税务文件服务（上传、覆盖、下载）
 */
@Service
@RequiredArgsConstructor
public class FileService {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final BlobStorageRemote blobStorageRemote;
    private final FileRecordMapper fileRecordMapper;
    private final PermissionService permissionService;

    /**
     * 上传文件
     */
    public FileRecord upload(String companyCode, String yearMonth, FileCategoryEnum category, MultipartFile file) throws IOException {
        permissionService.checkCompanyAccess(companyCode);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "empty file");
        }

        String originalName = file.getOriginalFilename();
        String filename = originalName == null ? "unknown.xlsx" : originalName;
        String blobPath = String.format("tax-ledger/%s/%s/%s/%s_%s",
                companyCode, yearMonth, category.name(), LocalDateTime.now().format(TS_FORMATTER), UUID.randomUUID());

        try (InputStream inputStream = file.getInputStream()) {
            blobStorageRemote.upload(blobPath, inputStream);
        }

        return saveOrReplace(companyCode, yearMonth, filename, category, blobPath, file.getSize());
    }

    /**
     * 同公司+账期+类别覆盖保存（旧记录逻辑删除）
     */
    public FileRecord saveOrReplace(String companyCode,
                                    String yearMonth,
                                    String fileName,
                                    FileCategoryEnum category,
                                    String blobPath,
                                    Long fileSize) {
        List<FileRecord> existed = fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, yearMonth)
                .eq(FileRecord::getFileCategory, category));

        existed.forEach(record -> {
            record.setIsDeleted(1);
            fileRecordMapper.updateById(record);
        });

        FileRecord record = new FileRecord();
        record.setCompanyCode(companyCode);
        record.setYearMonth(yearMonth);
        record.setFileName(fileName);
        record.setFileCategory(category);
        record.setBlobPath(blobPath);
        record.setFileSize(fileSize);
        record.setIsDeleted(0);
        fileRecordMapper.insert(record);
        return record;
    }

    /**
     * 列表查询
     */
    public List<FileRecord> list(String companyCode, String yearMonth) {
        permissionService.checkCompanyAccess(companyCode);
        return fileRecordMapper.selectList(new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getCompanyCode, companyCode)
                .eq(FileRecord::getYearMonth, yearMonth)
                .orderByDesc(FileRecord::getCreateTime));
    }

    /**
     * 下载文件
     */
    public void download(Long id, HttpServletResponse response) throws IOException {
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null || record.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "File not found");
        }

        permissionService.checkCompanyAccess(record.getCompanyCode());
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        blobStorageRemote.loadStream(record.getBlobPath(), response.getOutputStream());
    }
}
