package com.envision.bunny.infrastructure.filter.tracing;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;


import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @author jingjing.dong
 * @since 2021/3/17-15:49
 */
@Slf4j(topic = "Tracing Filter")
@Component
@Order(1)
public class TracingFilter extends OncePerRequestFilter {
    private static final List<RequestMatcher> matchers = new ArrayList<>();
    private static final org.springframework.util.AntPathMatcher PATH_MATCHER = new org.springframework.util.AntPathMatcher();
    //根据配置的ant patterns 进行初始化
    @PostConstruct
    public void initMatchers(){
        String[] antPatterns = Constants.ANT_PATTERNS.split(",");
        for (String antPattern : antPatterns) {
            matchers.add(request -> PATH_MATCHER.match(antPattern, request.getRequestURI()));
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, @NotNull HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        String traceId = httpServletRequest.getHeader(Constants.TRACE_ID_HEADER);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }
        MDC.put(Constants.TRACE_ID_MDC, traceId);
        MDC.put(Constants.USER, httpServletRequest.getRemoteUser());
        try {
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            MDC.remove(Constants.TRACE_ID_MDC);
            MDC.remove(Constants.USER);
        }
    }

    /**
     * Constants.antPatterns 定义的ant Pattern不会执行doFilterInternal
     */
    @Override
    protected boolean shouldNotFilter(@NotNull HttpServletRequest request) {
        for (RequestMatcher matcher : matchers) {
            if(matcher.matches(request)) {
                return true;
            }
        }
        return false;
    }


}
