package com.envision.epc.module.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/13-17:54
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ProjectValidator.class)
public @interface ValidProject {
    String message() default "{project.permission.error}";
    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
