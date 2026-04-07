package com.envision.epc.infrastructure.security;

import cn.hutool.core.net.url.UrlBuilder;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.URLUtil;
import com.envision.epc.infrastructure.security.okta.OktaProperties;
import com.envision.epc.infrastructure.security.wecom.WecomProperties;
import com.envision.epc.infrastructure.util.NetUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author jingjing.dong
 * @since 2021/4/1-10:37
 */
@Order(2)
@Component
@EnableConfigurationProperties({OktaProperties.class,WecomProperties.class})
@Slf4j
@RequiredArgsConstructor
public class LoginPortalFilter extends OncePerRequestFilter {
    static final String LOGIN_PORTAL_PATH = "/login";
    static final String STATE_PARAM = "state";
    static final String NONCE = "NONCE";
    static final String RESPONSE_TYPE = "code";
    static final String REDIRECT_SUFFIX = "#wechat_redirect";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final RequestMatcher MATCHER = (HttpServletRequest request) ->
            PATH_MATCHER.match(
                    LOGIN_PORTAL_PATH,
                    request.getRequestURI()
            );

    private final OktaProperties oktaProperties;

    private final WecomProperties wecomProperties;

    @Value("${custom.login.homepage.pc}")
    String pcHomepage;
    @Value("${custom.login.homepage.mobile}")
    String mobileHomepage;

    @Override
    protected void doFilterInternal(@NotNull HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse, @NotNull FilterChain filterChain) throws IOException {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String rawStateUrl = httpServletRequest.getParameter(STATE_PARAM);
        boolean isFromWecom = NetUtils.isFromWecom(httpServletRequest);
        boolean isFromPc = NetUtils.isFromPc(httpServletRequest);
        // 当判断是从PC端登录时，跳转到okta首页，否则跳转到移动端首页
        final String homepage = isFromPc ? pcHomepage : mobileHomepage;
        String stateUrl = StringUtils.isEmpty(rawStateUrl) ? homepage : buildSafeUrl(rawStateUrl, homepage);
        log.info("from wecom:[{}],from pc:[{}],state:[{}],redirectUrl:[{}]", isFromWecom, isFromPc, rawStateUrl, stateUrl);
        if (SecurityUtils.isAuthenticated(authentication)) {
            log.info("current user:[{}]", authentication.getName());
            httpServletResponse.sendRedirect(stateUrl);
            return;
        }
        String redirectUrl = isFromWecom ? buildWecomRedirectUrl(stateUrl) : buildOktaRedirectUrl(stateUrl);
        httpServletResponse.sendRedirect(redirectUrl);
    }

    private String buildOktaRedirectUrl(String stateUrl) {
        String clientId = oktaProperties.getClientId();
        return UrlBuilder.of(oktaProperties.getAuthorizationEndpoint())
                .addQuery("client_id", clientId)
                .addQuery("response_type", RESPONSE_TYPE)
                .addQuery("redirect_uri", oktaProperties.getRedirectUri())
                .addQuery("scope", oktaProperties.getScope())
                .addQuery("state", stateUrl)
                .addQuery("nonce", NONCE)
                .setCharset(StandardCharsets.UTF_8)
                .build();
    }

    private String buildWecomRedirectUrl(String stateUrl) {
        return UrlBuilder.of(wecomProperties.getAuthorizationEndpoint())
                .addQuery("appid", wecomProperties.getAppId())
                .addQuery("redirect_uri", wecomProperties.getRedirectUri())
                .addQuery("response_type", RESPONSE_TYPE)
                .addQuery("scope", wecomProperties.getScope())
                .addQuery("agentid", wecomProperties.getAgentId())
                .addQuery("state", stateUrl)
                .setCharset(StandardCharsets.UTF_8)
                .build() + REDIRECT_SUFFIX;
    }

    /**
     * 为了信息安全扫描
     *
     * @param url      跳转的地址
     * @param homepage 原定的首页地址
     * @return 安全的跳转地址
     */
    private String buildSafeUrl(String url, String homepage) {
        final String rawHost = URLUtil.url(url).getHost();
        final String safeHost = URLUtil.url(homepage).getHost();
        return CharSequenceUtil.replace(url, rawHost, safeHost);
    }

    /**
     * 仅Constants.PORTAL_PATH定义的url会执行doFilterInternal
     */
    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        return !MATCHER.matches(request);
    }
}

