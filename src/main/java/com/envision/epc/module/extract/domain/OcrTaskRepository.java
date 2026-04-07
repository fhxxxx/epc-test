package com.envision.epc.module.extract.domain;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:34
 */
public interface OcrTaskRepository extends IService<OcrTask> {
    List<OcrTask> queryBy(Long projectId, Long extractRunId, Integer version);

    List<OcrTask> queryBy(Long projectId, Long extractRunId, Integer version, Long fileId);

    List<OcrTask> queryBy(Long projectId, Long extractRunId, Integer version, Long fileId, OcrTaskStatusEnum status);

    OcrTask getBy(Long projectId, Long extractRunId, Integer version, Long fileId, Integer position);

    boolean updateById(OcrTask ocrTask, SFunction<OcrTask, ?>... column);

    boolean updateStatusBatch(Long extractRunId, Integer version, OcrTaskStatusEnum status, String error);

//    List<OcrTask> queryBy(Long projectId, Long extractRunVersionId, Long fileId);
}
