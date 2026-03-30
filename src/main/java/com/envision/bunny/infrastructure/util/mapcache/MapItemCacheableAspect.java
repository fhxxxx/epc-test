package com.envision.bunny.infrastructure.util.mapcache;

import com.envision.bunny.infrastructure.response.BizException;
import com.envision.bunny.infrastructure.response.ErrorCode;
import com.envision.bunny.infrastructure.util.SpELUtils;
import com.envision.bunny.infrastructure.util.redis.HashOps;
import com.envision.bunny.infrastructure.util.redis.KeyOps;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author wenjun.gu
 * @since 2021/11/18-14:51
 */
@Aspect
@Component
@DependsOn({"hashOps","keyOps"})
@SuppressWarnings("unchecked")
@Cacheable
public class MapItemCacheableAspect {

    @Around("@annotation(com.envision.bunny.infrastructure.util.mapcache.MapItemCacheable)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getMethod().getParameterTypes());
        MapItemCacheable mapItemCacheable = AnnotationUtils.findAnnotation(method, MapItemCacheable.class);
        String key = Objects.requireNonNull(mapItemCacheable).cacheName() + "::" + mapItemCacheable.key();
        String hKey = SpELUtils.simpleParse(mapItemCacheable.hKey(), method, joinPoint.getArgs());
        long expire = mapItemCacheable.expire();
        boolean allEntries = mapItemCacheable.allEntries();
        Object value;
        if (allEntries) {
            Map<String, Object> entries = HashOps.hGetAll(key);
            value = entries.size() == 0 ? null : entries;
        } else {
            value = HashOps.hGet(key, hKey);
        }
        if (value != null) {
            return value;
        }
        Object val;
        synchronized (this) {
            val = joinPoint.proceed();
            if (allEntries && !(val instanceof Map)) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR, "返回值必须是Map");
            }
            boolean hasKey = KeyOps.hasKey(key);
            if (allEntries) {
                HashOps.hPutAll(key, (Map<String, Object>) val);
            } else {
                HashOps.hPut(key, hKey, val);
            }
            if (!hasKey) {
                KeyOps.expire(key, expire, TimeUnit.SECONDS);
            }
            return val;
        }
    }
}
