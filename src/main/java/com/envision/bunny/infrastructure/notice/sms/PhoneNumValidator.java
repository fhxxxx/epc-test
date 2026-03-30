package com.envision.bunny.infrastructure.notice.sms;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 校验手机号是否格式正确
 * @author jingjing.dong
 * @since 2021/4/17-12:03
 */
public class PhoneNumValidator implements ConstraintValidator<PhoneNum, List<String>> {
    public static Pattern pattern = Pattern.compile("^1[0-9]{10}$");
    @Override
    public boolean isValid(List<String> value, ConstraintValidatorContext context) {
        if(Objects.isNull(value)||value.isEmpty()) {
            return false;
        }
        for (String num : value) {
            if (!pattern.matcher(num).matches()){
                return false;
            }
        }
        return true;
    }
}
