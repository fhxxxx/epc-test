package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2025-12-03
 */
public interface CompareRunDetailRepository extends IService<CompareRunDetail> {

    List<CompareRunDetail> getByCompareRunId(Long compareRunId);
}
