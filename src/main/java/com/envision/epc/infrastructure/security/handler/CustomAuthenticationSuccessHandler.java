package com.envision.epc.infrastructure.security.handler;

import com.envision.epc.infrastructure.security.SecurityUtils;
import com.envision.epc.infrastructure.util.HttpSessionUtils;
import com.envision.epc.module.user.domain.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.LocaleUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 用户认证通过的处理handler
 * @author jingjing.dong
 * @since 2021/4/6-17:26
 */
@Slf4j
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) throws IOException {
        CustomAuthenticationToken customAuthenticationToken = (CustomAuthenticationToken) authentication;
        final User currentUser = SecurityUtils.getCurrentUser();
        HttpSessionUtils.setLocale(LocaleUtils.toLocale(currentUser.getLocale()));
        httpServletResponse.sendRedirect(customAuthenticationToken.getSuccessUrl());
    }
}