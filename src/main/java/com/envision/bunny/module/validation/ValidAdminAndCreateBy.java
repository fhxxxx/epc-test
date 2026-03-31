package com.envision.bunny.module.validation;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/12-15:14
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = AdminAndCreateByValidator.class)
public @interface ValidAdminAndCreateBy {

    String message() default "{no.permission.error}";
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
