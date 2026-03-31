package com.envision.bunny.module.user.infrastructure;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envision.extract.module.user.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 系统用户表 Mapper 接口
 * </p>
 *
 * @author yakun.meng
 * @since 2024-05-09
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

}
