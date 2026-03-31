package com.envision.bunny.module.validation;

import com.envision.extract.infrastructure.security.SecurityUtils;
import com.envision.extract.module.permission.application.PermissionConfig;
import com.envision.extract.module.project.application.ProjectFacade;
import com.envision.extract.module.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

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
