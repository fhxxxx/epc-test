package com.envision.epc.infrastructure.util.ratelimit;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author wenjun.gu
 * @since 2021/12/15-14:14
 */
@Slf4j
@Aspect
@Component
public class RateLimiterAspect {
    @Autowired
    private StringRedisTemplate redisTemplate;
    private static DefaultRedisScript<Long> limitRedisScript;

    @Pointcut("@annotation(com.envision.epc.infrastructure.util.ratelimit.RateLimiter) || " +
            "@annotation(com.envision.epc.infrastructure.util.ratelimit.RateLimiters)")
    public void rateLimit() {
    }

    @Around("rateLimit()")
    public void pointcut(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Collection<RateLimiter> rateLimiters = AnnotatedElementUtils.getMergedRepeatableAnnotations(method, RateLimiter.class);
        for (RateLimiter rateLimiter : rateLimiters) {
            String key = rateLimiter.key();
            if (key.isEmpty()) {
                key = method.getDeclaringClass().getName() + "." + method.getName();
            }
            long limit = rateLimiter.limit();
            long timeout = rateLimiter.expire();
            int maxAttempt = rateLimiter.maxAttempt();
            long sleep = rateLimiter.sleep();
            RateLimiter.Policy policy = rateLimiter.policy();
            boolean limited = waitForLimit(key, limit, timeout, maxAttempt, sleep);
            if (limited) {
                switch (policy) {
                    case DISCARD: return;
                    case ABORT: throw new BizException(ErrorCode.RATE_LIMITED);
                    case ACCEPT: break;
                }
            }
        }
        point.proceed();
    }

    private boolean waitForLimit(String key, long limit, long expire, int maxAttempt, long sleep) throws InterruptedException {
        long executeTimes;
        int attempt = 0;
        do {
            long ttl = TimeUnit.SECONDS.toMillis(expire);
            long now = Instant.now().toEpochMilli();
            long expired = now - ttl;
            executeTimes = Objects.requireNonNull(redisTemplate.<Long>execute(limitRedisScript, Collections.singletonList(key),
                    String.valueOf(now), String.valueOf(ttl), String.valueOf(expired), String.valueOf(limit)),"Redis 返回结果不可能为 Null");
            attempt++;
            if (executeTimes == 0L) {
                log.info("[{}] 限流时长 [{}] 秒,已达到访问上限 [{}],最大尝试次数 [{}],正在重试 [{}]", key, expire, limit, maxAttempt, attempt);
                TimeUnit.MILLISECONDS.sleep(sleep);
            } else if (log.isDebugEnabled()) {
                log.debug("[{}] 限流时长 [{}] 秒,访问 [{}] 次", key, expire, executeTimes);
            }
        } while (executeTimes == 0L &&  attempt < maxAttempt);
        return attempt == maxAttempt && executeTimes == 0L;
    }

    @PostConstruct
    public static void init() {
        limitRedisScript = new DefaultRedisScript<>();
        limitRedisScript.setResultType(Long.class);
        limitRedisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("redis/SlideWindowRateLimiter.lua")));
        log.info("RateLimiter[分布式限流处理器]脚本加载完成");
    }
}