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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author wenjun.gu
 * @since 2021/11/19-12:47
 */
@Aspect
@Component
@DependsOn({"hashOps","keyOps"})
@SuppressWarnings("unchecked")
public class MapItemCachePutAspect {
    @Around("@annotation(com.envision.epc.infrastructure.util.mapcache.MapItemCachePut)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = joinPoint.getTarget().getClass().getMethod(signature.getName(), signature.getMethod().getParameterTypes());
        MapItemCachePut mapItemCachePut = AnnotationUtils.findAnnotation(method, MapItemCachePut.class);
        String key = Objects.requireNonNull(mapItemCachePut).cacheName() + "::" + mapItemCachePut.key();
        String hKey = SpELUtils.simpleParse(mapItemCachePut.hKey(), method, joinPoint.getArgs());
        long expire = mapItemCachePut.expire();
        boolean allEntries = mapItemCachePut.allEntries();
        Object val;
        synchronized (this) {
            val = joinPoint.proceed();
            if (allEntries) {
                HashOps.hPutAll(key, (Map<String, Object>) val);
            } else {
                HashOps.hPut(key, hKey, val);
            }
            KeyOps.expire(key, expire, TimeUnit.SECONDS);
            return val;
        }
    }
}
