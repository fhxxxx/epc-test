package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.TaxCompany;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TaxCompanyMapper extends BaseMapper<TaxCompany> {
}

