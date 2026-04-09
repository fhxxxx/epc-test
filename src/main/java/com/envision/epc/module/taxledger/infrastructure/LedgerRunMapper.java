package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.LedgerRun;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaxLedgerRunMapper 数据访问接口
 */
@Mapper
public interface LedgerRunMapper extends BaseMapper<LedgerRun> {
}


