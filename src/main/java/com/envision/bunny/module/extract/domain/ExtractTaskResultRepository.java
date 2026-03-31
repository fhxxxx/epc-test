package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author gangxiang.guan
 * @since 2025/9/22-11:07
 */
public interface ExtractTaskResultRepository extends IService<ExtractTaskResult> {
    ExtractTaskResult getBy(Long id, Long projectId, Long extractRunId, Integer version);

    List<ExtractTaskResult> getByTaskIds(List<Long> taskIds);

    void removeByCompositeIndex(Long projectId, Long compositeIndex);
}
