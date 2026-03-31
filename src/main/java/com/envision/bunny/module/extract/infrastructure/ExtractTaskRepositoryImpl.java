package com.envision.bunny.module.extract.infrastructure;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.extract.module.extract.domain.ExtractTask;
import com.envision.extract.module.extract.domain.ExtractTaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:40
 */
@Service
public class ExtractTaskRepositoryImpl extends ServiceImpl<ExtractTaskMapper, ExtractTask> implements ExtractTaskRepository {

    @Override
    public boolean updateById(ExtractTask extractTask, SFunction<ExtractTask, ?>... columns) {
        if (columns.length == 0) {
            return false;
        }
        LambdaUpdateChainWrapper<ExtractTask> eq = this.lambdaUpdate().eq(ExtractTask::getId, extractTask.getId());
        for (SFunction<ExtractTask, ?> column : columns) {
            eq = eq.set(column, column.apply(extractTask));
        }
        return eq.update();
    }

    @Override
    public List<ExtractTask> queryBy(Long projectId, Long extractRunId, Integer version) {
        return this.lambdaQuery()
                .eq(ExtractTask::getProjectId, projectId)
                .eq(ExtractTask::getExtractRunId, extractRunId)
                .eq(ExtractTask::getVersion, version)
                .list();
    }

    @Override
    public List<ExtractTask> queryBy(Long projectId, Long extractRunId, Integer version, Set<Long> fileIds) {
        return this.lambdaQuery()
                .eq(ExtractTask::getProjectId, projectId)
                .eq(ExtractTask::getExtractRunId, extractRunId)
                .eq(ExtractTask::getVersion, version)
                .in(CollUtil.isNotEmpty(fileIds), ExtractTask::getFileId, fileIds)
                .list();
    }

    @Override
    public ExtractTask queryById(Long id, Long projectId, Long extractRunId, Integer version) {
        return this.lambdaQuery()
                .eq(ExtractTask::getId, id)
                .eq(ExtractTask::getProjectId, projectId)
                .eq(ExtractTask::getExtractRunId, extractRunId)
                .eq(ExtractTask::getVersion, version)
                .one();
    }
}
