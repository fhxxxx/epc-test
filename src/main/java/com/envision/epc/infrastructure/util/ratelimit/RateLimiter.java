package com.envision.epc.infrastructure.util.ratelimit;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * @author wenjun.gu
 * @since 2021/12/15-14:11
 */
@Repeatable(RateLimiters.class)
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiter {
    /**
     * 限流key
     */
    @AliasFor("key")
    String value() default "";

    /**
     * 限流key
     */
    @AliasFor("value")
    String key() default "";

    /**
     * 最大请求数
     */
    long limit() default 10;

    /**
     * 限流时长/秒
     */
    long expire() default 60;

    /**
     * 最大重试次数
     */
    int maxAttempt() default 10;

    /**
     * 单次重试等待时长/毫秒
     */
    long sleep() default 500;

    /**
     * 到达最大重试次数后的策略
     */
    Policy policy() default Policy.ABORT;

    enum Policy{
        /**
         * 丢弃并抛出异常
         */
        ABORT,
        /**
         * 直接丢弃不抛出异常
         */
        DISCARD,
        /**
         * 允许运行
         */
        ACCEPT
    }
}