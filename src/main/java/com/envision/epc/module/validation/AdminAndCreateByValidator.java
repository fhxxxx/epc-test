package com.envision.epc.module.validation;

import com.envision.epc.infrastructure.security.SecurityUtils;
import com.envision.epc.module.permission.application.PermissionConfig;
import com.envision.epc.module.project.application.ProjectFacade;
import com.envision.epc.module.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/12-15:13
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AdminAndCreateByValidator implements ConstraintValidator<ValidAdminAndCreateBy, Long> {
    private final ProjectFacade projectFacade;
    private final PermissionConfig permissionConfig;
    @Override
    public boolean isValid(Long projectId, ConstraintValidatorContext context) {
        try {
            String userCode = SecurityUtils.getCurrentUserCode();
            Project project = projectFacade.getById(projectId);
            return permissionConfig.getAdmin().contains(userCode) || project.getCreateBy().equals(userCode);
        } catch (Exception e) {
            return false;
        }
    }
}
