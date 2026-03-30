package com.envision.bunny.infrastructure.security.handler;

import com.envision.bunny.infrastructure.response.Echo;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.JsonUtils;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 用户认证通过的处理handler,可以有多种方式
 * 1：可以自定义redirect的Url
 * 2：可以自定义返回体
 * @author jingjing.dong
 * @since 2021/4/6-17:26
 */
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException {
        httpServletResponse.setContentType("application/json;charset=utf-8");
        try(PrintWriter out = httpServletResponse.getWriter()) {
            out.write(JsonUtils.toJsonStr(Echo.fail(ErrorCode.AUTH_AUTHENTICATED_FAILURE)));
        }
    }
}
