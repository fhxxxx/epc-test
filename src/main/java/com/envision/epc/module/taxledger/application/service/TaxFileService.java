package com.envision.epc.module.taxledger.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileSourceEnum;
import com.envision.epc.module.taxledger.domain.TaxFileRecord;
import com.envision.epc.module.taxledger.infrastructure.TaxFileRecordMapper;
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
 * 税务文件管理服务（上传/下载/覆盖）
 */
@Service
@RequiredArgsConstructor
public class TaxFileService {
    private static final DateTimeFormatter TS_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private final BlobStorageRemote blobStorageRemote;
    private final TaxFileRecordMapper fileRecordMapper;
    private final TaxPermissionService permissionService;

    /**
     * 上传用户文件并落库
     */
    public TaxFileRecord upload(String companyCode, String yearMonth, FileCategoryEnum category, MultipartFile file) throws IOException {
        permissionService.checkCompanyAccess(companyCode);
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "empty file");
        }
        String originalName = file.getOriginalFilename();
        String filename = originalName == null ? "unknown.xlsx" : originalName;
        // 按公司+账期+类别分层存储，文件名加时间戳和UUID避免冲突
        String blobPath = String.format("tax-ledger/%s/%s/%s/%s_%s",
                companyCode, yearMonth, category.name(), LocalDateTime.now().format(TS_FORMATTER), UUID.randomUUID());

        try (InputStream inputStream = file.getInputStream()) {
            blobStorageRemote.upload(blobPath, inputStream);
        }
        return saveOrReplace(companyCode, yearMonth, filename, category, FileSourceEnum.UPLOAD, blobPath, file.getSize());
    }

    /**
     * 同键覆盖保存文件记录（旧记录逻辑删除）
     */
    public TaxFileRecord saveOrReplace(String companyCode, String yearMonth, String fileName, FileCategoryEnum category,
                                       FileSourceEnum source, String blobPath, Long fileSize) {
        List<TaxFileRecord> existed = fileRecordMapper.selectList(new LambdaQueryWrapper<TaxFileRecord>()
                .eq(TaxFileRecord::getIsDeleted, 0)
                .eq(TaxFileRecord::getCompanyCode, companyCode)
                .eq(TaxFileRecord::getYearMonth, yearMonth)
                .eq(TaxFileRecord::getFileCategory, category)
                .eq(TaxFileRecord::getFileSource, source));
        // 先删除旧版本记录，再插入新版本
        existed.forEach(record -> {
            record.setIsDeleted(1);
            fileRecordMapper.updateById(record);
        });

        TaxFileRecord record = new TaxFileRecord();
        record.setCompanyCode(companyCode);
        record.setYearMonth(yearMonth);
        record.setFileName(fileName);
        record.setFileCategory(category);
        record.setFileSource(source);
        record.setBlobPath(blobPath);
        record.setFileSize(fileSize);
        record.setUploadUser(permissionService.currentUserCode());
        record.setIsDeleted(0);
        fileRecordMapper.insert(record);
        return record;
    }

    /**
     * 按公司+账期查询文件列表
     */
    public List<TaxFileRecord> list(String companyCode, String yearMonth) {
        permissionService.checkCompanyAccess(companyCode);
        return fileRecordMapper.selectList(new LambdaQueryWrapper<TaxFileRecord>()
                .eq(TaxFileRecord::getIsDeleted, 0)
                .eq(TaxFileRecord::getCompanyCode, companyCode)
                .eq(TaxFileRecord::getYearMonth, yearMonth)
                .orderByDesc(TaxFileRecord::getCreateTime));
    }

    /**
     * 下载文件
     */
    public void download(Long id, HttpServletResponse response) throws IOException {
        TaxFileRecord record = fileRecordMapper.selectById(id);
        if (record == null || record.getIsDeleted() == 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "File not found");
        }
        permissionService.checkCompanyAccess(record.getCompanyCode());
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("UTF-8");
        String fileName = URLEncoder.encode(record.getFileName(), StandardCharsets.UTF_8).replace("+", "%20");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
        // 从Blob流式写回响应
        blobStorageRemote.loadStream(record.getBlobPath(), response.getOutputStream());
    }
}
