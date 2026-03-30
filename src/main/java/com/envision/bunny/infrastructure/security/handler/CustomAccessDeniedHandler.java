package com.envision.bunny.infrastructure.security.handler;

import com.envision.bunny.infrastructure.response.Echo;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

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
public class CustomAccessDeniedHandler implements AccessDeniedHandler {


    @Override
    public void handle(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
                       AccessDeniedException e) {
        httpServletRequest.removeAttribute("org.springframework.boot.web.servlet.error.DefaultErrorAttributes.ERROR");
        httpServletResponse.setContentType("application/json;charset=utf-8");
        try(PrintWriter out = httpServletResponse.getWriter()) {
            out.write(JsonUtils.toJsonStr(Echo.fail(ErrorCode.AUTH_ACCESS_DENIED)));
        } catch (IOException ioException) {
            log.error("access denied post process error",ioException);
        }
    }
}
