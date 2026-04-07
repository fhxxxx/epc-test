package com.envision.epc.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.epc.module.extract.domain.ExtractRun;
import com.envision.epc.module.extract.domain.ExtractRunRepository;
import org.springframework.stereotype.Service;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:39
 */
@Service
public class ExtractRunRepositoryImpl extends ServiceImpl<ExtractRunMapper, ExtractRun> implements ExtractRunRepository {
    @Override
    public boolean updateById(ExtractRun extractRun, SFunction<ExtractRun, ?>... columns) {
        if (columns.length == 0) {
            return false;
        }
        LambdaUpdateChainWrapper<ExtractRun> eq = this.lambdaUpdate().eq(ExtractRun::getId, extractRun.getId());
        for (SFunction<ExtractRun, ?> column : columns) {
            eq = eq.set(column, column.apply(extractRun));
        }
        return eq.update();
    }

}
