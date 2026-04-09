package com.envision.epc.module.taxledger.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.epc.module.taxledger.domain.UserPermission;
import org.apache.ibatis.annotations.Mapper;

/**
 * TaxUserPermissionMapper 数据访问接口
 */
@Mapper
public interface UserPermissionMapper extends BaseMapper<UserPermission> {
}


