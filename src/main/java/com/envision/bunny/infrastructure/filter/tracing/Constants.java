package com.envision.bunny.infrastructure.filter.tracing;

/**
 * @author jingjing.dong
 * @since 2021/4/16-11:24
 */
public class Constants {
    //静态文件 actuator 验证码
    static final String ANT_PATTERNS = "/static/**,/actuator/**,/verifyCode,/login/**,/mail/**";
    static final String TRACE_ID_HEADER = "x-trace-id";
    static final String TRACE_ID_MDC = "trace_id";
    static final String USER = "user";
}
