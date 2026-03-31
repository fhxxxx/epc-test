package com.envision.bunny.module.user.application;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envision.extract.infrastructure.mybatis.BasicPagination;
import com.envision.extract.infrastructure.response.BizException;
import com.envision.extract.infrastructure.response.ErrorCode;
import com.envision.extract.infrastructure.security.SecurityUtils;
import com.envision.extract.infrastructure.web.domain.BaseQueryService;
import com.envision.extract.module.permission.application.PermissionConfig;
import com.envision.extract.module.user.application.dto.UserAssembler;
import com.envision.extract.module.user.application.dto.UserDto;
import com.envision.extract.module.user.domain.User;
import com.envision.extract.module.user.domain.UserRepository;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;


/**
 * @author yakun.meng
 * @since 2024/5/9-15:18
 */
@Service
@EnableConfigurationProperties(PermissionConfig.class)
public class UserQueryService extends BaseQueryService<UserRepository, User> {

    @Resource
    private UserAssembler assembler;

    @Resource
    private PermissionConfig permissionConfig;

    /**
     * 登录时使用
     * @param userCode account
     * @return 用户
     */
    public User getByUserCode(String userCode) {
        User user = super.repository.lambdaQuery().eq(User::getInService, true).eq(User::getUserCode, userCode).one();
        if (user == null) {
            throw new BizException(ErrorCode.AUTH_ACCESS_DENIED);

        }

        if (permissionConfig.getAdmin().contains(user.getUserCode())) {
            return user;
        }

        if (!permissionConfig.getDivisions().isEmpty()
                && !permissionConfig.getDivisions().contains(user.getDivisionCode())) {
            throw new BizException(ErrorCode.AUTH_ACCESS_DENIED);
        }

        return user;
    }

    public User getCurrentUserInfo() {
        return SecurityUtils.getCurrentUser();
    }

    public List<User> list(String fuzzySearch) {
        return repository.lambdaQuery().like(User::getSearchStr, fuzzySearch).last("limit 10").list();
    }

    public BasicPagination<UserDto> queryUser(UserKeywordQuery query) {
        Page<User> page = super.repository
                .lambdaQuery()
                .ne(User::getInService, false)
                .and(CharSequenceUtil.isNotBlank(query.getUsername()), q ->
                        q.like(User::getUsername, query.getUsername())
                                .or()
                                .like(User::getUserCode, query.getUsername())
                                .or()
                                .like(User::getAccount, query.getUsername())
                )
                .page(new Page<>(query.getPageNum(), query.getPageSize()));
        return BasicPagination.of(page, assembler::toDto);
    }
}
