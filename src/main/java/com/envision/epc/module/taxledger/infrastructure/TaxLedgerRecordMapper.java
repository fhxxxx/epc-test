package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.TaxLedgerRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaxLedgerRecordMapper 数据访问接口
 */
@Mapper
public interface TaxLedgerRecordMapper extends BaseMapper<TaxLedgerRecord> {
}


