package com.envision.epc.infrastructure.util.ratelimit;

import java.lang.annotation.*;

/**
 * @author wenjun.gu
 * @since 2021/12/17-14:52
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimiters {
    RateLimiter[] value();
}
