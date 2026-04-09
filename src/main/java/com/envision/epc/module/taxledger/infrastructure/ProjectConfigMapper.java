package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.ProjectConfig;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaxProjectConfigMapper 数据访问接口
 */
@Mapper
public interface ProjectConfigMapper extends BaseMapper<ProjectConfig> {
}


