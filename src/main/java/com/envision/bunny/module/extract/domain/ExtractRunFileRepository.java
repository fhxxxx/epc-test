package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/3-11:59
 */
public interface ExtractRunFileRepository extends IService<ExtractRunFile> {

    boolean updateById(ExtractRunFile extractRunFile, SFunction<ExtractRunFile, ?>... columns);

    boolean updateBatchById(Collection<ExtractRunFile> extractRunVersions, SFunction<ExtractRunFile, ?>... columns);

    List<ExtractRunFile> queryBy(Long projectId, Long extractRunId, Integer version);

    List<ExtractRunFile> queryBy(Long projectId, Long extractRunId, Integer version, String companyCode);

    List<ExtractRunFile> queryWithVersionIds(List<Long> runVersionIds);

    boolean updateStatusBatch(Long extractRunId, Integer version, ExtractRunStatusEnum status, String error);
}
