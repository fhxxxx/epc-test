package com.envision.epc.infrastructure.util.mapcache;

import org.springframework.core.annotation.AliasFor;

import jakarta.validation.constraints.NotNull;
import java.lang.annotation.*;

/**
 * @author wenjun.gu
 * @since 2021/11/18-14:50
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MapItemCacheEvict {
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
     * 是否对整个map操作
     */
    boolean allEntries() default false;
}
