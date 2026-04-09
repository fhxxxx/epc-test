package com.envision.epc.module.validation;

import com.envision.epc.infrastructure.security.SecurityUtils;
import com.envision.epc.module.taxledger.common.TaxLedgerProperties;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 项目权限校验
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectValidator implements ConstraintValidator<ValidProject, Long> {
//    private final ProjectFacade projectFacade;
    private final TaxLedgerProperties taxLedgerProperties;
    private static final String ORIGINAL_TEXT = "errorMessage";

    @Override
    public boolean isValid(Long projectId, ConstraintValidatorContext constraintValidatorContext) {
        HibernateConstraintValidatorContext context = constraintValidatorContext.unwrap(HibernateConstraintValidatorContext.class);
        try {
            String userCode = SecurityUtils.getCurrentUserCode();
//            Project project = projectFacade.getById(projectId);
//
//            // 已按要求移除“项目授权列表(projectIdsByUserId)”维度。
//            if (taxLedgerProperties.getSuperAdminUserCodes().contains(userCode)
//                    || project.getCreateBy().equals(userCode)) {
//                return true;
//            }
            context.addMessageParameter(ORIGINAL_TEXT, "你没有该项目的权限");
            return false;
        } catch (Exception e) {
            context.addMessageParameter(ORIGINAL_TEXT, "项目不存在");
            return false;
        }
    }
}
