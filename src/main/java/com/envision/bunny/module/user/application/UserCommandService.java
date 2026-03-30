package com.envision.bunny.module.user.application;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.envision.bunny.facade.platform.PlatformRemote;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.security.SecurityUtils;
import com.envision.bunny.infrastructure.web.domain.BaseCommandService;
import com.envision.bunny.module.user.domain.User;
import com.envision.bunny.module.user.domain.UserRepository;
import com.envision.bunny.module.user.infrastructure.Constant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author yakun.meng
 * @since 2024/4/30
 */
@Service
@Slf4j
public class UserCommandService extends BaseCommandService<UserRepository, User> {

    @Resource
    private PlatformRemote platformRemote;
    @Value("${custom.platform.token.domain}")
    private String platformDomain;


    public void switchLocale(String locale) {
        if (!Constant.LOCALES.contains(locale)) {
            throw new BizException(ErrorCode.UNSUPPORTED_LANGUAGE);
        }
        User currentUser = repository.getById(SecurityUtils.getCurrentUserId());
        currentUser.setLocale(locale);
        repository.lambdaUpdate().eq(User::getUserId, currentUser.getUserId()).set(User::getLocale, locale).update();
        SecurityUtils.setChangedUser(currentUser);
    }


    @Transactional
    public void syncWithDataLake() {
        final String yesterday = DateUtil.yesterday().toDateStr();
        final String reqUrl = platformDomain + CharSequenceUtil.format(Constant.OA_USER_REQ_PATH_PATTERN, yesterday);
        List<User> remoteUsers = platformRemote.fetchFromDataLake(Constant.OA_USER_SVC, reqUrl, User::fromPltData);
        if (CollUtil.isEmpty(remoteUsers)) {
            return;
        }
        final List<String> remoteUserCode = remoteUsers.stream().map(User::getUserCode).collect(Collectors.toList());
        List<User> dbUsers = repository.lambdaQuery().in(User::getUserCode,remoteUserCode).list();
        Map<String, User> dbUserMap = dbUsers.stream().collect(Collectors.toMap(User::getUserCode, Function.identity()));
        for (User remoteUser : remoteUsers) {
            User tempUser = dbUserMap.get(remoteUser.getUserCode());
            remoteUser.setUserId(tempUser == null ? null : tempUser.getUserId());
            remoteUser.setLocale(tempUser == null ? "zh_CN" : tempUser.getLocale());
        }
        repository.saveOrUpdateBatch(remoteUsers);
    }

    @Transactional
    public void syncAllUser() {
        final String reqUrl = platformDomain + Constant.ALL_OA_USER_REQ_PATH_PATTERN;
        List<User> remoteUsers = platformRemote.fetchFromDataLake(Constant.OA_USER_SVC, reqUrl, User::fromPltData);
        if (CollUtil.isEmpty(remoteUsers)) {
            return;
        }
        repository.saveBatch(remoteUsers);
    }

}
