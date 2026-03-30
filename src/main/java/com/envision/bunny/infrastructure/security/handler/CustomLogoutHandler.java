package com.envision.bunny.infrastructure.security.handler;

import com.envision.bunny.infrastructure.response.Echo;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 用户认证通过的处理handler
 *
 * @author jingjing.dong
 * @since 2021/4/6-17:26
 */
@Slf4j
public class CustomLogoutHandler implements LogoutSuccessHandler {

    @Override
    public void onLogoutSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
        httpServletResponse.setContentType("application/json;charset=utf-8");
        try (PrintWriter out = httpServletResponse.getWriter()) {
            out.write(JsonUtils.toJsonStr(Echo.fail(ErrorCode.AUTH_LOGOUT)));
        } catch (IOException ioException) {
            log.error("log out error:", ioException);
        }
    }
}
