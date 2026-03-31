package com.envision.bunny.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.extract.module.extract.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:43
 */
@Service
public class OcrTaskRepositoryImpl extends ServiceImpl<OcrTaskMapper, OcrTask> implements OcrTaskRepository {
    @Override
    public List<OcrTask> queryBy(Long projectId, Long extractRunId, Integer version) {
        return this.lambdaQuery()
                .eq(OcrTask::getProjectId, projectId)
                .eq(OcrTask::getExtractRunId, extractRunId)
                .eq(OcrTask::getVersion, version)
                .list();
    }

    @Override
    public List<OcrTask> queryBy(Long projectId, Long extractRunId, Integer version, Long fileId) {
        return this.lambdaQuery()
                .eq(OcrTask::getProjectId, projectId)
                .eq(OcrTask::getExtractRunId, extractRunId)
                .eq(OcrTask::getVersion, version)
                .eq(OcrTask::getFileId, fileId)
                .list();
    }

    @Override
    public List<OcrTask> queryBy(Long projectId, Long extractRunId, Integer version, Long fileId, OcrTaskStatusEnum status) {
        return this.lambdaQuery()
                .eq(OcrTask::getProjectId, projectId)
                .eq(OcrTask::getExtractRunId, extractRunId)
                .eq(OcrTask::getVersion, version)
                .eq(OcrTask::getFileId, fileId)
                .eq(OcrTask::getStatus, status)
                .list();
    }

    @Override
    public OcrTask getBy(Long projectId, Long extractRunId, Integer version, Long fileId, Integer position) {
        return this.lambdaQuery()
                .eq(OcrTask::getProjectId, projectId)
                .eq(OcrTask::getExtractRunId, extractRunId)
                .eq(OcrTask::getVersion, version)
                .eq(OcrTask::getFileId, fileId)
                .eq(OcrTask::getPosition, position)
                .one();
    }

    @Override
    public boolean updateById(OcrTask ocrResult, SFunction<OcrTask, ?>... columns) {
        if (columns.length == 0) {
            return false;
        }
        LambdaUpdateChainWrapper<OcrTask> eq = this.lambdaUpdate().eq(OcrTask::getId, ocrResult.getId());
        for (SFunction<OcrTask, ?> column : columns) {
            eq = eq.set(column, column.apply(ocrResult));
        }
        return eq.update();
    }

    @Override
    public boolean updateStatusBatch(Long extractRunId, Integer version, OcrTaskStatusEnum status, String error) {
        return this.lambdaUpdate().eq(OcrTask::getExtractRunId, extractRunId)
                .eq(OcrTask::getVersion, version)
                .set(OcrTask::getStatus, status)
                .set(OcrTask::getError, error).update();
    }
}
