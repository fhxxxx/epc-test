package com.envision.bunny.infrastructure.crypto.param;

import java.lang.annotation.*;

/**
 * @author jingjing.dong
 * @since 2023/4/2-13:14
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
public @interface DecryptAnnotation {
}

