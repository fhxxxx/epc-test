package com.envision.bunny.module.extract.application.validations;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.text.CharSequenceUtil;
import com.envision.extract.infrastructure.util.MsgUtils;
import com.envision.extract.module.extract.application.query.InvoiceQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/15-12:01
 */
@Component
@EnableConfigurationProperties(CompareValidation.class)
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class InvoiceValidator implements ConstraintValidator<ValidInvoice, InvoiceQuery> {
    private final CompareValidation compareValidation;

    @Override
    public boolean isValid(InvoiceQuery value, ConstraintValidatorContext context) {
        context.disableDefaultConstraintViolation();
        if (CollUtil.isEmpty(value.getCompanyCodeList())) {
            context.buildConstraintViolationWithTemplate("公司代码不能为空").addConstraintViolation();
            return false;
        } else if (value.getCompanyCodeList().size() > compareValidation.getMaxCompanyCodeCount()) {
            context.buildConstraintViolationWithTemplate(MsgUtils.getMessage("max.company.code.count.error",
                    compareValidation.getMaxCompanyCodeCount())).addConstraintViolation();
            return false;
        } else if (value.getCompanyCodeList().stream().anyMatch(code -> code.length() != compareValidation.getCompanyCodeLimit())) {
            context.buildConstraintViolationWithTemplate(MsgUtils.getMessage("company.code.limit.error",
                    compareValidation.getCompanyCodeLimit())).addConstraintViolation();
            return false;
        }
        if (CharSequenceUtil.isBlank(value.getFiscalYearPeriodStart()) || CharSequenceUtil.isBlank(value.getFiscalYearPeriodEnd())) {
            context.buildConstraintViolationWithTemplate("会计年度/期间不能为空").addConstraintViolation();
            return false;
        }
        //前4位数字，第5位是0，最后2位是01-16
        if (!value.getFiscalYearPeriodStart().matches("\\d{4}0(0[1-9]|1[0-6])") ||
                !value.getFiscalYearPeriodEnd().matches("\\d{4}0(0[1-9]|1[0-6])")) {
            context.buildConstraintViolationWithTemplate("会计年度/期间格式不正确").addConstraintViolation();
            return false;
        }
        // 如果传了日期的话 校验下日期格式是否正确
        if (CharSequenceUtil.isNotBlank(value.getPostingDateInTheDocumentStart()) &&
                CharSequenceUtil.isNotBlank(value.getPostingDateInTheDocumentEnd())) {
            LocalDate startDate;
            LocalDate endDate;
            try {
                startDate = LocalDate.parse(value.getPostingDateInTheDocumentStart(), DateTimeFormatter.ofPattern("yyyyMMdd"));
                endDate = LocalDate.parse(value.getPostingDateInTheDocumentEnd(), DateTimeFormatter.ofPattern("yyyyMMdd"));
            } catch (Exception e) {
                context.buildConstraintViolationWithTemplate("过账日期格式不正确").addConstraintViolation();
                return false;
            }
            if (startDate.isAfter(endDate)) {
                context.buildConstraintViolationWithTemplate("结束日期不能小于开始日期").addConstraintViolation();
                return false;
            }
            List<LocalDate> dateRange = value.getDateRange();
            if (dateRange.get(0).isAfter(startDate) || dateRange.get(1).isBefore(startDate)
                    || dateRange.get(0).isAfter(endDate) || dateRange.get(1).isBefore(endDate)) {
                context.buildConstraintViolationWithTemplate("过账日期需属于所选的会计期间").addConstraintViolation();
                return false;
            }
        }
        return true;
    }
}
