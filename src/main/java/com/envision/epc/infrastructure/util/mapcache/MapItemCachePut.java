package com.envision.epc.infrastructure.util.mapcache;

import org.springframework.core.annotation.AliasFor;

import jakarta.validation.constraints.NotNull;
import java.lang.annotation.*;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapItemCachePut {
    /**
     * redis cacheName
     */
    @AliasFor("cacheName")
    String value() default "";

    /**
     * redis cacheName
     */
    @AliasFor("value")
    String cacheName() default "";

    /**
     * redis key
     */
    @NotNull
    String key();

    /**
     * hash key
     */
    String hKey() default "";

    /**
     * 过期时间/秒
     */
    long expire() default 25 * 60 * 60L;

    /**
     * 是否对整个map操作
     */
    boolean allEntries() default false;
}
