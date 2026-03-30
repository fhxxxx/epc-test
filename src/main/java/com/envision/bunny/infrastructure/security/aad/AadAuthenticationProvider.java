package com.envision.bunny.infrastructure.security.aad;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.jwt.JWT;
import cn.hutool.jwt.JWTUtil;
import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.security.handler.CustomAuthenticationToken;
import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import com.envision.bunny.infrastructure.util.RestClientUtils;
import com.envision.bunny.module.user.application.UserQueryService;
import com.envision.bunny.module.user.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2021/4/1-11:34
 */
@Slf4j
@Component("aadAuthProvider")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AadAuthenticationProvider implements AuthenticationProvider {
    private final UserQueryService userQueryService;
    private final RestClient restClient;
    private final AadProperties properties;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken aadToken = (CustomAuthenticationToken) authentication;
        final AadAuthenticationProvider provider = ApplicationContextUtils.getBean(AadAuthenticationProvider.class);
        log.info("AAD Authentication code:[{}]", aadToken.getCode());
        String token = provider.queryToken(aadToken);
        String userAccount = provider.queryUserAccount(token);
        User user = userQueryService.getByAccount(userAccount);
        return createSuccessAuthentication(user, authentication, aadToken.getSuccessUrl());
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return (CustomAuthenticationToken.class.isAssignableFrom(aClass));
    }

    @Retryable(retryFor = IOException.class, backoff = @Backoff(delay = 500L, multiplier = 1.5))
    public String queryToken(CustomAuthenticationToken aadToken) {
        TokenResp resp = restClient.post()
                .uri(properties.getTokenEndpoint())
                .body(aadToken)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body(TokenResp.class);
        return Objects.requireNonNull(resp, "internal error when query token").getAccessToken();
    }
    @Recover
    public String recover(IOException e, CustomAuthenticationToken aadToken) {
        log.warn("query token failed：{}", e.getMessage());
        throw new BizException(ErrorCode.IGNORE_ALARM);
    }


    @NotNull
    private HttpEntity<MultiValueMap<String, Object>> getHttpEntity(CustomAuthenticationToken aadToken, HttpHeaders headers) {
        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("code", aadToken.getCode());
        paramMap.add("grant_type", "authorization_code");
        paramMap.add("client_id", properties.getClientId());
        paramMap.add("client_secret", properties.getClientSecret());
        paramMap.add("redirect_uri", properties.getRedirectUri());
        paramMap.add("scope", properties.getScope());
        return new HttpEntity<>(paramMap, headers);
    }

    /**
     * 因为AAD的token获取接口有可能失败，所以这里重试，必须public方法
     * 获取userinfo接口是验证access token的有效性
     * @param token access token
     * @return account
     */
    @Retryable(retryFor = IOException.class, backoff = @Backoff(delay = 500L, multiplier = 1.5))
    public String queryUserAccount(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Authorization", String.format("Bearer %s", token));
        RestClientUtils.getWithHeaders(properties.getUserinfoEndpoint(), headers,UserinfoResp.class);
        return extractAccount(token);
    }

    @Nullable
    private static String extractAccount(String token) {
        JWT jwt = JWTUtil.parseToken(token);
        Map<String, Object> payloadClaims = jwt.getPayloads();
        final String upn = payloadClaims.get("upn").toString();
        return CharSequenceUtil.subBefore(upn, "@", false);
    }

    private Authentication createSuccessAuthentication(User user, Authentication authentication, String successUrl) {
        CustomAuthenticationToken result = new CustomAuthenticationToken(user, successUrl, Collections.emptyList());
        result.setDetails(authentication.getDetails());
        return result;
    }

    @Setter
    @Getter
    private static class TokenResp {
        @JsonProperty("token_type")
        private String tokenType;
        @JsonProperty("expires_in")
        private int expiresIn;
        @JsonProperty("access_token")
        private String accessToken;
        private String scope;
        @JsonProperty("id_token")
        private String idToken;
    }

    @Setter
    @Getter
    private static class UserinfoResp {
        private String sub;  // AAD用户唯一标识符
        private String email;
        private String name;
    }
}
