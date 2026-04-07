package com.envision.epc.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.epc.module.extract.domain.ExtractRunStatusEnum;
import com.envision.epc.module.extract.domain.ExtractRunVersion;
import com.envision.epc.module.extract.domain.ExtractRunVersionRepository;
import org.springframework.stereotype.Service;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:54
 */
@Service
public class ExtractRunVersionRepositoryImpl extends ServiceImpl<ExtractRunVersionMapper, ExtractRunVersion> implements ExtractRunVersionRepository {
    @Override
    public boolean updateById(ExtractRunVersion extractRunVersion, SFunction<ExtractRunVersion, ?>... columns) {
        if (columns.length == 0) {
            return false;
        }
        LambdaUpdateChainWrapper<ExtractRunVersion> eq = this.lambdaUpdate().eq(ExtractRunVersion::getId, extractRunVersion.getId());
        for (SFunction<ExtractRunVersion, ?> column : columns) {
            eq = eq.set(column, column.apply(extractRunVersion));
        }
        return eq.update();
    }

    @Override
    public ExtractRunVersion getBy(Long projectId, Long extractRunId, Integer currentVersion) {
        return this.lambdaQuery()
                .eq(ExtractRunVersion::getProjectId, projectId)
                .eq(ExtractRunVersion::getExtractRunId, extractRunId)
                .eq(ExtractRunVersion::getVersion, currentVersion)
                .one();
    }

    @Override
    public IPage<ExtractRunVersion> queryExtractRunWithStatus(IPage<ExtractRunVersion> page, Long projectId, ExtractRunStatusEnum status) {
        return this.baseMapper.queryExtractRunWithStatus(page, projectId, status);
    }
}