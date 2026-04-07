package com.envision.epc.infrastructure.security.handler;

import com.envision.epc.infrastructure.response.Echo;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.infrastructure.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 用户认证通过的处理handler
 * @author jingjing.dong
 * @since 2021/4/6-17:26
 */
@Slf4j
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) {
        httpServletResponse.setContentType("application/json;charset=utf-8");
        try(PrintWriter out = httpServletResponse.getWriter()) {
            out.write(JsonUtils.toJsonStr(Echo.fail(ErrorCode.AUTH_UNAUTHENTICATED)));
        } catch (IOException ioException) {
            log.error("login error ", ioException);
        }
    }
}
