package com.envision.bunny.module.uploadfile.application;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envision.extract.facade.azure.BlobStorageRemote;
import com.envision.extract.infrastructure.mybatis.BasicPagination;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.infrastructure.util.MsgUtils;
import com.envision.extract.module.extract.application.validations.CompareValidation;
import com.envision.extract.module.uploadfile.application.dtos.UploadFileDTO;
import com.envision.extract.module.uploadfile.application.query.KeywordQuery;
import com.envision.extract.module.uploadfile.domain.UploadFile;
import com.envision.extract.module.uploadfile.domain.UploadFileRepository;
import com.envision.extract.module.uploadfile.domain.UploadTypeEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Optional;

/**
 * @author wenjun.gu
 * @since 2025/8/14-11:33
 */
@Slf4j
@Service
@EnableConfigurationProperties(CompareValidation.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class UploadFileQueryService {
    private final UploadFileRepository uploadFileRepository;
    private final UploadFileAssembler uploadFileAssembler;
    private final BlobStorageRemote blobStorageRemote;
    private final CompareValidation compareValidation;

    public void filePreview(Long fileId, HttpServletResponse response, HttpServletRequest request) {
        Optional<UploadFile> optById = uploadFileRepository.getOptById(fileId);
        if (optById.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "文件不存在");
        }
        UploadFile uploadFile = optById.get();
        try {
            long totalSize = uploadFile.getSize();
            String range = request.getHeader("Range");
            long start = 0;
            long end = totalSize - 1;
            boolean partial = false;
            if (range != null && range.matches("^bytes=\\d*-\\d*$")) {
                partial = true;
                String[] parts = range.replace("bytes=", "").split("-");
                start = parts[0].isEmpty() ? 0 : Long.parseLong(parts[0]);
                end = parts[1].isEmpty() ? totalSize - 1 : Long.parseLong(parts[1]);

                if (start < 0 || start >= totalSize || start > end) {
                    response.setHeader("Content-Range", "bytes */" + totalSize);
                    response.sendError(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return;
                }
                if (end >= totalSize) {
                    end = totalSize - 1;
                }
            }
            long contentLength = end - start + 1;
            if (partial) {
                response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                response.setHeader("Content-Range", "bytes " + start + "-" + end + "/" + totalSize);
            } else {
                response.setStatus(HttpServletResponse.SC_OK);
            }
            response.setHeader("Content-Length", String.valueOf(contentLength));
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Expose-Headers", "Accept-Ranges, Content-Range, Content-Length");
            response.setHeader("Accept-Ranges", "bytes");
            response.setContentType("application/pdf");
            response.setHeader("Connection", "keep-alive");
            response.setHeader("Keep-Alive", "timeout=5");
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");
            ServletOutputStream outputStream = response.getOutputStream();
            blobStorageRemote.loadStream(uploadFile.getPath(), start, contentLength, outputStream);
//            outputStream.close();
        } catch (Exception e) {
            throw new BizException(ErrorCode.BAD_REQUEST, "下载文件失败");
        }
    }

    public BasicPagination<UploadFileDTO> queryFiles(Long projectId, KeywordQuery query) {
        Page<UploadFile> page = uploadFileRepository.lambdaQuery()
                .eq(UploadFile::getProjectId, projectId)
                .in(CollUtil.isNotEmpty(query.getTypeList()), UploadFile::getType, query.getTypeList())
                .like(CharSequenceUtil.isNotBlank(query.getCompanyCode()), UploadFile::getCompanyCode, query.getCompanyCode())
                .and(CharSequenceUtil.isNotBlank(query.getKeyword()), i -> i
                        .like(UploadFile::getCompanyCode, query.getKeyword()).or()
                .like(UploadFile::getName, query.getKeyword()))
                .orderByDesc(UploadFile::getCreateTime)
                .page(new Page<>(query.getPageNum(), query.getPageSize()));

        return BasicPagination.of(page, uploadFileAssembler::toUploadFileDTO);
    }

    public List<UploadFileDTO> queryAllFiles(Long projectId, KeywordQuery query) {
        return uploadFileRepository.lambdaQuery()
                .eq(UploadFile::getProjectId, projectId)
                .in(CollUtil.isNotEmpty(query.getTypeList()), UploadFile::getType, query.getTypeList())
                .and(CharSequenceUtil.isNotBlank(query.getKeyword()), i -> i
                        .like(UploadFile::getCompanyCode, query.getKeyword()).or()
                        .like(UploadFile::getName, query.getKeyword()))
                .orderByDesc(UploadFile::getCompanyCode).list().stream().map(uploadFileAssembler::toUploadFileDTO).toList();
    }

    public List<UploadFile> validAndGetFiles(List<Long> idList, UploadTypeEnum type, Long projectId) {
        if (idList.size() > compareValidation.getMaxFileCount()) {
            throw new BizException(ErrorCode.BAD_REQUEST, MsgUtils.getMessage("max.file.count.error", compareValidation.getMaxFileCount()));
        }
        List<UploadFile> resultExcels = uploadFileRepository.getFile(idList, type, projectId);
        if (idList.size() != resultExcels.size()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "未找到" + type.getName());
        }
//        resultExcels.forEach(result -> {
//            if (!blobStorageRemote.exists(result.getPath())) {
//                throw new BizException(ErrorCode.BAD_REQUEST, "未找到" + type.getName() + ":" + result.getId());
//            }
//        });
        return resultExcels;
    }
}
