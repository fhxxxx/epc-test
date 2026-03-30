package com.envision.bunny.infrastructure.lock;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.SpELUtils;
import com.envision.bunny.infrastructure.util.redis.LockOps;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.UUID;

/**
 * @author jingjing.dong
 * @since 2023/8/5-22:51
 */
@Aspect
@Component
@Slf4j
public class LockAspect {
    @Autowired
    private LockOps lockOps;

    @Pointcut("@annotation(com.envision.bunny.infrastructure.lock.Lock))")
    public void locks() {
    }

    @Around("locks()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();
        SpELUtils spELUtils = new SpELUtils(method, args);
        Lock lock = AnnotationUtils.getAnnotation(method, Lock.class);
        if (Objects.isNull(lock)) {
            return null;
        }
        String key = spELUtils.parseExpression(lock.key());
        boolean flag = false;
        String lockValue = UUID.randomUUID().toString();
        try {
            flag = LockOps.getLockUntilTimeout(key, lockValue, lock.timeout(), lock.unit(), lock.retryTimeLimit());
            if (flag) {
                return point.proceed();
            } else {
                throw new BizException(ErrorCode.LOCK_ALREADY_OCCUPIED, key);
            }
        } finally {
            if (flag) {
                LockOps.releaseLock(key, lockValue);
            }
        }


    }
}
