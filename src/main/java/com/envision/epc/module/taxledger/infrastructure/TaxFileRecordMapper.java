package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.TaxFileRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaxFileRecordMapper 数据访问接口
 */
@Mapper
public interface TaxFileRecordMapper extends BaseMapper<TaxFileRecord> {
}


