package com.envision.epc.module.extract.application.validations;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author wenjun.gu
 * @since 2025/9/15-12:00
 */
@Target({ElementType.FIELD})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = ExtractConfigValidator.class)
public @interface ValidExtractConfig {
    String message() default "{extract.config.validate.error}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
