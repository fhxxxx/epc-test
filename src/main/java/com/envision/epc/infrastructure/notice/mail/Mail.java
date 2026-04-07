package com.envision.epc.infrastructure.notice.mail;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author jingjing.dong
 * @since 2021/4/17-11:49
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER,ElementType.TYPE })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {MailValidator.class })
public @interface Mail {
    String message() default "至少包含一个收件人";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
