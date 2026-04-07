package com.envision.epc.infrastructure.notice.whitelist;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author jingjing.dong
 * @since 2021/4/17-11:49
 */
@Target({ElementType.METHOD})
@Retention(RUNTIME)
@Documented
public @interface Whitelist {
}
