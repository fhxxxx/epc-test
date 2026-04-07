package com.envision.epc.module.extract.infrastructure;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.epc.module.extract.domain.CompareRunDetail;
import com.envision.epc.module.extract.domain.CompareRunDetailRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author jingjing.dong
 * @since 2025-12-03
 */
@Service
public class CompareRunDetailRepositoryImpl extends ServiceImpl<CompareRunDetailMapper, CompareRunDetail> implements CompareRunDetailRepository {

    @Override
    public List<CompareRunDetail> getByCompareRunId(Long compareRunId) {
        QueryWrapper<CompareRunDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("compare_run_id", compareRunId);
        queryWrapper.orderByDesc("create_time");

        return this.baseMapper.selectList(queryWrapper);
    }
}
