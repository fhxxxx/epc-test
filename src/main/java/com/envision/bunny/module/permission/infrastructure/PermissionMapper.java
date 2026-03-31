package com.envision.bunny.module.permission.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.extract.module.permission.domain.Permission;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:10
 */
@Mapper
public interface PermissionMapper extends BaseMapper<Permission> {
}
