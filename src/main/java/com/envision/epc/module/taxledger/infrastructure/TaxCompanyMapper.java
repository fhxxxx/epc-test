package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.TaxCompany;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaxCompanyMapper 数据访问接口
 */
@Mapper
public interface TaxCompanyMapper extends BaseMapper<TaxCompany> {
}


