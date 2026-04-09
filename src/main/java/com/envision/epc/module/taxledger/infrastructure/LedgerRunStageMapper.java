package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.LedgerRunStage;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaxLedgerRunStageMapper 数据访问接口
 */
@Mapper
public interface LedgerRunStageMapper extends BaseMapper<LedgerRunStage> {
}


