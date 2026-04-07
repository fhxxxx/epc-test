package com.envision.epc.module.uploadfile.domain;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:32
 */
public interface UploadFileRepository extends IService<UploadFile> {
    UploadFile getByHash(String hash);

    UploadFile getFile(Long id, UploadTypeEnum type, Long projectId);

    List<UploadFile> getFile(List<Long> idList, UploadTypeEnum type, Long projectId);
}
