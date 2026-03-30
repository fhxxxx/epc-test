package com.envision.bunny.infrastructure.security.wecom;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.security.handler.CustomAuthenticationToken;
import com.envision.bunny.infrastructure.util.HttpSessionUtils;
import com.envision.bunny.module.user.application.UserQueryService;
import com.envision.bunny.module.user.domain.User;
import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2021/4/1-11:34
 */
@Component("wecomAuthProvider")
@EnableConfigurationProperties(WecomProperties.class)
@Slf4j
@Profile("anonymous")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class WecomAuthenticationProviderLocal implements AuthenticationProvider {
    private final UserQueryService userQueryService;
    private final RestClient restClient;
    private final WecomProperties properties;

    @Value("${custom.login.wecom.whitelist}")
    List<String> whiteList;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken wecomToken = (CustomAuthenticationToken) authentication;
        final String code = wecomToken.getCode();
        log.info("code[{}]", code);
        String userId;
        if (whiteList.contains(code)) {
            userId = code;
        } else {
            userId = queryUserId(code);
        }
        log.info("user[{}]", userId);
        User user = userQueryService.getByUserCode(userId);
        HttpSessionUtils.setFromWxwork(true);
        return createSuccessAuthentication(user, wecomToken, wecomToken.getSuccessUrl());
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return (CustomAuthenticationToken.class
                .isAssignableFrom(aClass));
    }

    /**
     * 参考Thunder团队提供的包装过的Api
     * <a href="http://kbse.pd.envisioncn.com/wiki/pages/viewpage.action?pageId=18914272">...</a>
     *
     * @param code 企业微信跳转带的code
     * @return 获取到的UserID即员工号
     */
    private String queryUserId(String code){
        Map<String,String> params = ImmutableMap.of("code",code,"secret",properties.getClientSecret());
        UserinfoResp resp = restClient.post()
                .uri(properties.getAuthorizationEndpoint(), params)
                .retrieve()
                .body(UserinfoResp.class);
        if (Objects.requireNonNull(resp,"internal error when query userinfo").getErrcode() != 0) {
            throw new BizException(ErrorCode.WECOM_USERINFO_ERROR, resp.getErrmsg());
        }
        return  resp.getData().getUserId();
    }

    private Authentication createSuccessAuthentication(User user, Authentication authentication, String successUrl) {
        CustomAuthenticationToken result = new CustomAuthenticationToken(user, successUrl, Collections.emptyList());
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Setter
    @Getter
    private static class UserinfoResp {
        private int errcode;
        private String errmsg;
        private Data data;
    }

    /**
     * Auto-generated: 2021-04-13 18:27:2
     */
    @Getter
    @Setter
    private static class Data {
        private String userId;
        private String userName;
        private String domainAccount;

    }
}
