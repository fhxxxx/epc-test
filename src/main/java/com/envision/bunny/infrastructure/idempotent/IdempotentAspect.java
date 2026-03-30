package com.envision.bunny.infrastructure.idempotent;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.SpELUtils;
import com.envision.bunny.infrastructure.util.redis.LockOps;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * @author jingjing.dong
 * @since 2021/3/26-10:07
 */
@Aspect
@Component
public class IdempotentAspect {
    @Autowired
    StringRedisTemplate redisTemplate;
    private static final String KEY_TEMPLATE = "idempotent_%S";
    private static final String DEFAULT_LOCK_VALUE = "1";

    /**
     * 自定义幂等注解
     */
    @Pointcut("@annotation(com.envision.bunny.infrastructure.idempotent.Idempotent)")
    public void pointcut() {
    }

    /**
     * 进行幂等检查的切面
     */
    @Before("pointcut()")
    public void doBefore(JoinPoint jPoint) throws BizException {
        //获取当前方法信息
        Method method = ((MethodSignature) jPoint.getSignature()).getMethod();
        //获取注解
        Idempotent idempotent = method.getAnnotation(Idempotent.class);
        //获取参数的所有值。
        Object[] args = jPoint.getArgs();
        String prefix = idempotent.prefix();
        if (prefix.isEmpty()) {
            prefix = method.getDeclaringClass().getCanonicalName() + method.getName();
        }
        String argsStr = SpELUtils.simpleParse(idempotent.spelKey(), method, args);
        String cacheKey = String.format(KEY_TEMPLATE, prefix + "_" + argsStr);
        if (!acquireDistributeLock(cacheKey, idempotent.expireSeconds())) {
            throw new BizException(ErrorCode.IDEMPOTENT_VALIDATE_FAIL);
        }
    }


    private boolean acquireDistributeLock(String key, int timeout) {
         return LockOps.getLock(key, DEFAULT_LOCK_VALUE, timeout, TimeUnit.SECONDS);
    }

}

