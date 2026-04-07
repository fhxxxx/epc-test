package com.envision.epc.module.extract.domain;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:33
 */
public interface ExtractRunRepository extends IService<ExtractRun> {
    boolean updateById(ExtractRun extractRun, SFunction<ExtractRun, ?>... columns);

}
