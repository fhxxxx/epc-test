package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;
import org.apache.ibatis.annotations.Param;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:53
 */
public interface ExtractRunVersionRepository extends IService<ExtractRunVersion> {
    boolean updateById(ExtractRunVersion extractRunVersion, SFunction<ExtractRunVersion, ?>... columns);

    ExtractRunVersion getBy(Long projectId, Long extractRunId, Integer currentVersion);

    IPage<ExtractRunVersion> queryExtractRunWithStatus(IPage<ExtractRunVersion> page, @Param("projectId") Long projectId, @Param("status") ExtractRunStatusEnum status);

}
