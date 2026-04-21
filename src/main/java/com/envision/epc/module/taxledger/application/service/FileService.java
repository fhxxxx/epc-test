package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.domain.CompanyCodeConfig;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.infrastructure.CompanyCodeConfigMapper;
import com.envision.epc.module.taxledger.infrastructure.FileRecordMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
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
    private final CompanyCodeConfigMapper companyCodeConfigMapper;
    private final PermissionService permissionService;

    /**
     * 上传文件
     */
    public FileRecord upload(String companyCode, String yearMonth, FileCategoryEnum category, MultipartFile file) throws IOException {
        permissionService.checkCompanyAccess(companyCode);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "empty file");
        }
        if (category == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "fileCategory is required");
        }
        if (!isValidCompanyCode(companyCode)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "companyCode not found");
        }

        String originalName = file.getOriginalFilename();
        String filename = originalName == null ? "unknown.xlsx" : originalName;
        validateXlsx(file, filename);

        String blobPath = String.format("tax-ledger/%s/%s/%s/%s_%s.xlsx",
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

    private boolean isValidCompanyCode(String companyCode) {
        return companyCodeConfigMapper.selectCount(new LambdaQueryWrapper<CompanyCodeConfig>()
                .eq(CompanyCodeConfig::getIsDeleted, 0)
                .eq(CompanyCodeConfig::getCompanyCode, companyCode)) > 0;
    }

    private void validateXlsx(MultipartFile file, String filename) {
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "only .xlsx file is allowed");
        }
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            return;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        boolean validType =
                normalized.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                        || normalized.contains("application/octet-stream");
        if (!validType) {
            throw new BizException(ErrorCode.BAD_REQUEST, "invalid file content type");
        }
    }

    /**
     * 列表查询
     */
    public List<FileRecord> list(String companyCode, String yearMonth) {
        LambdaQueryWrapper<FileRecord> queryWrapper = new LambdaQueryWrapper<FileRecord>()
                .eq(FileRecord::getIsDeleted, 0)
                .eq(FileRecord::getYearMonth, yearMonth)
                .orderByDesc(FileRecord::getCreateTime);

        if (StringUtils.hasText(companyCode)) {
            permissionService.checkCompanyAccess(companyCode);
            queryWrapper.eq(FileRecord::getCompanyCode, companyCode);
            return fileRecordMapper.selectList(queryWrapper);
        }

        if (!permissionService.canAccessAllCompanies()) {
            List<String> grantedCompanyCodes = permissionService.listGrantedCompanyCodes();
            if (grantedCompanyCodes.isEmpty()) {
                return List.of();
            }
            queryWrapper.in(FileRecord::getCompanyCode, grantedCompanyCodes);
        }

        return fileRecordMapper.selectList(queryWrapper);
    }

    public void deleteFiles(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "ids is empty");
        }
        ids.stream().filter(Objects::nonNull).distinct().forEach(this::deleteFile);
    }

    public void deleteFile(Long id) {
        if (id == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "id is empty");
        }
        FileRecord record = fileRecordMapper.selectById(id);
        if (record == null || record.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "File not found");
        }

        permissionService.checkCompanyAccess(record.getCompanyCode());
        record.setIsDeleted(1);
        fileRecordMapper.updateById(record);

        try {
            blobStorageRemote.delete(record.getBlobPath());
        } catch (Exception ignore) {
            // 主流程以逻辑删除成功为准
        }
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
