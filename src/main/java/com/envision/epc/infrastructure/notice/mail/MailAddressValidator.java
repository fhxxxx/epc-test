package com.envision.epc.infrastructure.notice.mail;

import cn.hutool.core.util.StrUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 校验邮箱是否格式正确
 *
 * @author jingjing.dong
 * @since 2021/4/17-12:03
 */
public class MailAddressValidator implements ConstraintValidator<MailAddress, List<String>> {
    private static final Pattern pattern = Pattern.compile("^(\\w+([-.][A-Za-z0-9]+)*){3,18}@\\w+([-.][A-Za-z0-9]+)*\\.\\w+([-.][A-Za-z0-9]+)*$");

    @Override
    public boolean isValid(List<String> addressArr, ConstraintValidatorContext context) {
        for (String address : addressArr) {
            if (StrUtil.isEmpty(address)){
                return false;
            }
            if (!pattern.matcher(address).matches()) {
                return false;
            }
        }
        return true;
    }
}
