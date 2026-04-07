package com.envision.epc.module.extract.infrastructure;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.epc.module.extract.domain.ExtractRunFile;
import com.envision.epc.module.extract.domain.ExtractRunFileRepository;
import com.envision.epc.module.extract.domain.ExtractRunStatusEnum;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/4-19:44
 */
@Service
public class ExtractRunFileRepositoryImpl extends ServiceImpl<ExtractRunFileMapper, ExtractRunFile> implements ExtractRunFileRepository {
    @Override
    public boolean updateById(ExtractRunFile extractRunFile, SFunction<ExtractRunFile, ?>... columns) {
        if (columns.length == 0) {
            return false;
        }
        LambdaUpdateChainWrapper<ExtractRunFile> eq = this.lambdaUpdate().eq(ExtractRunFile::getId, extractRunFile.getId());
        for (SFunction<ExtractRunFile, ?> column : columns) {
            eq = eq.set(column, column.apply(extractRunFile));
        }
        return eq.update();
    }


    @Override
    public boolean updateBatchById(Collection<ExtractRunFile> extractRunVersions, SFunction<ExtractRunFile, ?>... columns) {
        if (columns.length == 0) {
            return false;
        }
        return this.executeBatch(extractRunVersions, (sqlSession, entity) -> {
            LambdaUpdateChainWrapper<ExtractRunFile> updateWrapper = this.lambdaUpdate()
                    .eq(ExtractRunFile::getId, entity.getId());
            for (SFunction<ExtractRunFile, ?> column : columns) {
                updateWrapper.set(column, column.apply(entity));
            }
            updateWrapper.update();
        });
    }

    @Override
    public List<ExtractRunFile> queryBy(Long projectId, Long extractRunId, Integer version) {
        return this.lambdaQuery()
                .eq(ExtractRunFile::getProjectId, projectId)
                .eq(ExtractRunFile::getExtractRunId, extractRunId)
                .eq(ExtractRunFile::getVersion, version)
                .list();
    }

    @Override
    public List<ExtractRunFile> queryBy(Long projectId, Long extractRunId, Integer version, String companyCode) {
        return this.lambdaQuery()
                .eq(ExtractRunFile::getProjectId, projectId)
                .eq(ExtractRunFile::getExtractRunId, extractRunId)
                .eq(CharSequenceUtil.isNotBlank(companyCode), ExtractRunFile::getCompanyCode, companyCode)
                .eq(ExtractRunFile::getVersion, version)
                .list();
    }

    @Override
    public List<ExtractRunFile> queryWithVersionIds(List<Long> runVersionIds) {
        return this.baseMapper.queryWithVersionIds(runVersionIds);
    }

    @Override
    public boolean updateStatusBatch(Long extractRunId, Integer version, ExtractRunStatusEnum status, String error) {
        return this.lambdaUpdate().eq(ExtractRunFile::getExtractRunId, extractRunId)
                .eq(ExtractRunFile::getVersion, version)
                .set(ExtractRunFile::getStatus, status)
                .set(ExtractRunFile::getError, error).update();
    }
}
