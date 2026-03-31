package com.envision.bunny.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.extract.module.extract.domain.CompareRun;
import com.envision.extract.module.extract.domain.CompareRunRepository;
import org.springframework.stereotype.Service;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:54
 */
@Service
public class CompareRunRepositoryImpl extends ServiceImpl<CompareRunMapper, CompareRun> implements CompareRunRepository {


    @Override
    public IPage<CompareRun> getByPage(Long projectId, Page<CompareRun> page) {
        QueryWrapper<CompareRun> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("project_id", projectId);
        queryWrapper.orderByDesc("create_time");

        return this.baseMapper.selectPage(page, queryWrapper);
    }

    @Override
    public boolean updateById(CompareRun compareRun, SFunction<CompareRun, ?>... columns) {
        if (columns.length == 0) {
            return false;
        }
        LambdaUpdateChainWrapper<CompareRun> eq = this.lambdaUpdate().eq(CompareRun::getId, compareRun.getId());
        for (SFunction<CompareRun, ?> column : columns) {
            eq = eq.set(column, column.apply(compareRun));
        }
        return eq.update();
    }
}