package com.envision.epc.module.extract.infrastructure;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.epc.module.extract.domain.ExtractTaskResult;
import com.envision.epc.module.extract.domain.ExtractTaskResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author gangxiang.guan
 * @since 2025/9/22-11:08
 */
@Service
public class ExtractTaskResultRepositoryImpl extends ServiceImpl<ExtractTaskResultMapper, ExtractTaskResult> implements ExtractTaskResultRepository {
    @Override
    public ExtractTaskResult getBy(Long id, Long projectId, Long extractRunId, Integer version) {
        return this.lambdaQuery()
                .eq(ExtractTaskResult::getId, id)
                .eq(ExtractTaskResult::getProjectId, projectId)
                .eq(ExtractTaskResult::getExtractRunId, extractRunId)
                .eq(ExtractTaskResult::getVersion, version)
                .one();
    }

    @Override
    public List<ExtractTaskResult> getByTaskIds(List<Long> taskIds) {
        return this.lambdaQuery()
                .in(ExtractTaskResult::getExtractTaskId, taskIds)
                .list();
    }

    @Override
    public void removeByCompositeIndex(Long projectId, Long compositeIndex) {
        this.lambdaUpdate().eq(ExtractTaskResult::getProjectId, projectId)
                .eq(ExtractTaskResult::getCompositeIndex, compositeIndex).remove();
    }
}