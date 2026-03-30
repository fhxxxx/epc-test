package com.envision.bunny.infrastructure.security.okta;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.security.handler.CustomAuthenticationToken;
import com.envision.bunny.infrastructure.util.ApplicationContextUtils;
import com.envision.bunny.module.user.application.UserQueryService;
import com.envision.bunny.module.user.domain.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
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
import java.util.Objects;

/**
 * @author jingjing.dong
 * @since 2021/4/1-11:34
 */
@Slf4j
@Component("oktaAuthProvider")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class OktaAuthenticationProvider implements AuthenticationProvider {
    private final UserQueryService userQueryService;
    private final RestClient restClient;
    private final OktaProperties properties;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        CustomAuthenticationToken oktaToken = (CustomAuthenticationToken) authentication;
        final OktaAuthenticationProvider provider = ApplicationContextUtils.getBean(OktaAuthenticationProvider.class);
        log.info("OKTA Authentication code:[{}]", oktaToken.getCode());
        String token = provider.queryToken(oktaToken);
        String userId = provider.queryUserId(token);
        User user = userQueryService.getByUserCode(userId);
        return createSuccessAuthentication(user, authentication, oktaToken.getSuccessUrl());
    }

    @Override
    public boolean supports(Class<?> aClass) {
        return (CustomAuthenticationToken.class
                .isAssignableFrom(aClass));
    }

    @Retryable(retryFor = IOException.class, backoff = @Backoff(delay = 500L, multiplier = 1.5))
    public String queryToken(CustomAuthenticationToken oktaToken) {
        final MultiValueMap<String, Object> entity = getBody(oktaToken);
        TokenResp resp = restClient.post()
                .uri(properties.getTokenEndpoint())
                .body(entity)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .retrieve()
                .body(TokenResp.class);
        return Objects.requireNonNull(resp, "internal error when query token").getAccessToken();
    }

    @NotNull
    private MultiValueMap<String, Object> getBody(CustomAuthenticationToken oktaToken) {
        MultiValueMap<String, Object> paramMap = new LinkedMultiValueMap<>();
        paramMap.add("code", oktaToken.getCode());
        paramMap.add("grant_type", Constants.GRANT_TYPE);
        paramMap.add("client_id", properties.getClientId());
        paramMap.add("client_secret", properties.getClientSecret());
        paramMap.add("redirect_uri", properties.getRedirectUri());
        return paramMap;
    }

    @Retryable(retryFor = IOException.class, backoff = @Backoff(delay = 500L, multiplier = 1.5))
    public String queryUserId(String token) {
        UserinfoResp resp = restClient.post()
                .uri(properties.getUserinfoEndpoint())
                .header("Authorization", String.format("Bearer %s", token))
                .retrieve()
                .body(UserinfoResp.class);
        return Objects.requireNonNull(resp, "internal error when query userinfo").getEid();
    }

    @Recover
    public String recover(IOException e, CustomAuthenticationToken oktaToken) {
        log.warn("query token failed：{}", e.getMessage());
        throw new BizException(ErrorCode.IGNORE_ALARM);
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
        private String sub;
        private String eid;
    }
}
