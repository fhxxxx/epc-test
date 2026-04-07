package com.envision.epc.module.extract.domain;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:53
 */
public interface CompareRunRepository extends IService<CompareRun> {

    IPage<CompareRun> getByPage(Long projectId, Page<CompareRun> page);

    boolean updateById(CompareRun compareRun, SFunction<CompareRun, ?>... columns);

}
