package com.envision.epc.infrastructure.util.mapcache;

import com.envision.epc.infrastructure.util.SpELUtils;
import com.envision.epc.infrastructure.util.redis.HashOps;
import com.envision.epc.infrastructure.util.redis.KeyOps;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Objects;

/**
 * @author wenjun.gu
 * @since 2021/11/18-14:57
 */
@Aspect
@Component
@DependsOn({"hashOps","keyOps"})
public class MapItemCacheEvictAspect {

    @Around("@annotation(com.envision.epc.infrastructure.util.mapcache.MapItemCacheEvict)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getMethod().getParameterTypes());
        MapItemCacheEvict mapItemCacheEvict = AnnotationUtils.findAnnotation(method, MapItemCacheEvict.class);
        String key = Objects.requireNonNull(mapItemCacheEvict).cacheName() + "::" + mapItemCacheEvict.key();
        String hKey = SpELUtils.simpleParse(mapItemCacheEvict.hKey(), method, joinPoint.getArgs());
        boolean allEntries = mapItemCacheEvict.allEntries();
        Object val;
        synchronized (this) {
            val = joinPoint.proceed();
            if (allEntries) {
                KeyOps.delete(key);
            } else {
                HashOps.hDelete(key, hKey);
            }
            return val;
        }
    }
}
