package com.envision.bunny.infrastructure.log;

import java.lang.annotation.*;

/**
 * 不打日志注解
 * @author wenjun.gu
 * @since 2024/3/7-11:14
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AvoidLog {
}
