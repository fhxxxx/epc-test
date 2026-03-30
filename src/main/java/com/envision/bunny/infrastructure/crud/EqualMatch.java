package com.envision.bunny.infrastructure.crud;

import java.lang.annotation.*;

/**
 * @author jingjing.dong
 * @since 2023/3/29-17:25
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EqualMatch {
}
