package com.envision.bunny.module.extract.application.validations;

import javax.validation.Constraint;
import javax.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author gangxiang.guan
 * @date 2025/9/26 11:34
 */
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = InvoiceValidator.class)
public @interface ValidInvoice {

    String message() default "{extract.config.validate.error}";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
