package com.envision.epc.infrastructure.log;

import com.envision.epc.infrastructure.security.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/**
 * 日志切面，对所有通过Scheduled注解注释的方法打日志
 * @author jingjing.dong
 * @since 2021/3/23-10:39
 */
@Component
@Slf4j(topic = "background")
@Aspect
public class CronLogAspect {
    private static final String TRACE_ID_MDC = "trace_id";
    private static final String USER = "user";

    @Around("@annotation(org.springframework.scheduling.annotation.Scheduled) && !@annotation(com.envision.epc.infrastructure.log.AvoidLog)")
    public void printCronLog(ProceedingJoinPoint joinPoint) throws Throwable {
        MDC.put(TRACE_ID_MDC, UUID.randomUUID().toString());
        MDC.put(USER, "background");
        // 保存原始上下文（避免影响线程池中的其他任务）
        SecurityContext originalContext = SecurityContextHolder.getContext();
        try {
            String methodName = joinPoint.getSignature().getDeclaringTypeName() + "." + joinPoint.getSignature().getName();
            log.info("begin run background {} at {}", methodName, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
            // 手动创建系统用户的认证信息
            SecurityContextHolder.getContext().setAuthentication(SecurityUtils.BACKGROUND_AUTHENTICATION_TOKEN);
            joinPoint.proceed();
            log.info("end run background {} at {}", methodName, LocalDateTime.now(ZoneId.of("Asia/Shanghai")));
        } finally {
            // 恢复原始上下文（重要！防止线程复用导致的信息泄露）
            SecurityContextHolder.setContext(originalContext);
            MDC.remove(TRACE_ID_MDC);
            MDC.remove(USER);
        }
    }
}
