package com.envision.epc.module.validation;

import com.envision.epc.infrastructure.security.SecurityUtils;
//import com.envision.epc.module.project.application.ProjectFacade;
//import com.envision.epc.module.project.domain.Project;
import com.envision.epc.module.taxledger.common.TaxLedgerProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * admin 或项目创建人校验
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminAndCreateByValidator implements ConstraintValidator<ValidAdminAndCreateBy, Long> {
//    private final ProjectFacade projectFacade;
    private final TaxLedgerProperties taxLedgerProperties;

    @Override
    public boolean isValid(Long projectId, ConstraintValidatorContext context) {
        try {
            String userCode = SecurityUtils.getCurrentUserCode();
//            Project project = projectFacade.getById(projectId);
//            return taxLedgerProperties.getSuperAdminUserCodes().contains(userCode)
//                    || project.getCreateBy().equals(userCode);
            return true;//todo
        } catch (Exception e) {
            return false;
        }
    }
}
