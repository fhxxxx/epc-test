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
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {MailAddressValidator.class })
public @interface MailAddress {
    String message() default "邮件格式不正确";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
