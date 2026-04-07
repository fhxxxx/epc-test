package com.envision.epc.infrastructure.lock;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;


/**
 * @author jingjing.dong
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Lock {
    /**
     * 加锁id
     */
    @AliasFor("key")
    String value() default "";

    /**
     * 限流key
     */
    @AliasFor("value")
    String key() default "";

    /**
     * 时长
     */
    long timeout() default 10;

    /**
     * 时间单位
     */
    TimeUnit unit() default TimeUnit.SECONDS;

    /**
     * 重试超时时间
     */
    long retryTimeLimit() default 2000;
}
