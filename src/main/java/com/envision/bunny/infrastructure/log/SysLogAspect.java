package com.envision.bunny.infrastructure.log;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.Optional;

/**
 * @author jingjing.dong
 * @since 2021/3/17-18:39
 */
@Slf4j(topic="call in")
@Aspect
@Component
public class SysLogAspect {
    static final ThreadLocal<Long> startTime = new ThreadLocal<>();

    // 本来希望用 @target 但是启动失败，报错和shedLock 冲突
    @Pointcut("@within(org.springframework.web.bind.annotation.RestController) && !@annotation(com.envision.bunny.infrastructure.log.AvoidLog)")
    public void webLog() {
    }

    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }
        HttpServletRequest request = attributes.getRequest();
        String url = request.getRequestURL().toString();
        String httpMethod = request.getMethod();
        String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
        String args = Arrays.toString(joinPoint.getArgs());
        SysLog aLog = SysLog.builder().url(url).methodName(methodName).httpMethod(httpMethod).args(args).build();
        startTime.set(System.currentTimeMillis());
        log.info(aLog.toString());
    }

    @AfterReturning(returning = "resp", pointcut = "webLog()")
    public void doAfterReturning(Object resp) {
        long cost = System.currentTimeMillis() - startTime.get();
        log.info(String.format("response=[%s],cost time=[%s]", Optional.ofNullable(resp).orElse(
                "void"), cost));
    }
}
