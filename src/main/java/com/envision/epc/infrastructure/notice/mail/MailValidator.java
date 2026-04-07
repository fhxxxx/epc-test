package com.envision.epc.infrastructure.notice.mail;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 必须至少有一个接收邮箱
 *
 * @author jingjing.dong
 * @since 2021/4/17-12:03
 */
public class MailValidator implements ConstraintValidator<Mail, MailBody> {
    @Override
    public boolean isValid(MailBody body, ConstraintValidatorContext context) {
        boolean noRecipients = body.getTo().isEmpty() && body.getCc().isEmpty() && body.getBcc().isEmpty();
        return !noRecipients;
    }
}
