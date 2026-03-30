package com.envision.bunny.module.user.application;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.security.SecurityUtils;
import com.envision.bunny.infrastructure.web.domain.BaseQueryService;
import com.envision.bunny.module.user.application.dto.UserAssembler;
import com.envision.bunny.module.user.domain.User;
import com.envision.bunny.module.user.domain.UserRepository;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Optional;


/**
 * @author yakun.meng
 * @since 2024/5/9-15:18
 */
@Service
public class UserQueryService extends BaseQueryService<UserRepository, User> {

    @Resource
    private UserAssembler assembler;

    /**
     * 登录时使用
     * @param userCode 员工号
     * @return 用户
     */
    public User getByUserCode(String userCode) {
        final Optional<User> oneOpt = super.repository.lambdaQuery().eq(User::getInService, true).eq(User::getUserCode, userCode).oneOpt();
        if (oneOpt.isPresent()) {
            return oneOpt.get();
        } else {
            throw new BizException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }
    /**
     * 登录时使用
     * @param account 域账号
     * @return 用户
     */
    public User getByAccount(String account) {
        final Optional<User> oneOpt = super.repository.lambdaQuery().eq(User::getInService, true).eq(User::getAccount, account).oneOpt();
        if (oneOpt.isPresent()) {
            return oneOpt.get();
        } else {
            throw new BizException(ErrorCode.AUTH_ACCESS_DENIED);
        }
    }

    public User getCurrentUserInfo() {
        return SecurityUtils.getCurrentUser();
    }

    public List<User> list(String fuzzySearch) {
        return repository.lambdaQuery().like(User::getSearchStr, fuzzySearch).last("limit 10").list();
    }
}
