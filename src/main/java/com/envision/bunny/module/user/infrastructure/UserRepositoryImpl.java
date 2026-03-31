package com.envision.bunny.module.user.infrastructure;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.extract.module.user.domain.User;
import com.envision.extract.module.user.domain.UserRepository;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 系统用户表 服务实现类
 * </p>
 *
 * @author yakun.meng
 * @since 2024-05-09
 */
@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
    @Override
    public User getByUserCode(String userCode) {
        return this.lambdaQuery()
                .eq(User::getUserCode, userCode)
                .one();
    }
}
