package com.envision.epc.module.user.domain;

import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 系统用户表 服务类
 * </p>
 *
 * @author yakun.meng
 * @since 2024-05-09
 */
public interface UserRepository extends IService<User> {
    User getByUserCode(String userCode);
}
