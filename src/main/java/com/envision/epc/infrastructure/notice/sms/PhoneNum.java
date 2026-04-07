package com.envision.epc.infrastructure.notice.sms;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.*;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author jingjing.dong
 * @since 2021/4/17-11:49
 */
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RUNTIME)
@Documented
@Constraint(validatedBy = {PhoneNumValidator.class })
public @interface PhoneNum {
    String message() default "发送号码数字有误";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
