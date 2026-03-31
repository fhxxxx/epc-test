package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Set;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:33
 */
public interface ExtractTaskRepository extends IService<ExtractTask> {

    boolean updateById(ExtractTask extractTask, SFunction<ExtractTask, ?>... columns);

    List<ExtractTask> queryBy(Long projectId, Long extractRunId, Integer version);

    List<ExtractTask> queryBy(Long projectId, Long extractRunId, Integer version, Set<Long> fileIds);

    ExtractTask queryById(Long id, Long projectId, Long extractRunId, Integer version);
}
