package com.envision.bunny.module.validation;

import com.envision.extract.infrastructure.security.SecurityUtils;
import com.envision.extract.module.permission.application.PermissionConfig;
import com.envision.extract.module.permission.application.PermissionFacade;
import com.envision.extract.module.project.application.ProjectFacade;
import com.envision.extract.module.project.domain.Project;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/13-17:53
 */
@Component
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectValidator implements ConstraintValidator<ValidProject, Long> {
    private final ProjectFacade projectFacade;
    private final PermissionFacade permissionFacade;
    private final PermissionConfig permissionConfig;
    private static final String ORIGINAL_TEXT = "errorMessage";


    @Override
    public boolean isValid(Long projectId, ConstraintValidatorContext constraintValidatorContext) {
        HibernateConstraintValidatorContext context = constraintValidatorContext.unwrap(HibernateConstraintValidatorContext.class);
        try{
            String userCode = SecurityUtils.getCurrentUserCode();
            List<Long> projectIdsByUserId = permissionFacade.getProjectIdsByUserId(userCode);
            Project project = projectFacade.getById(projectId);
            if (permissionConfig.getAdmin().contains(userCode) || project.getCreateBy().equals(userCode) || projectIdsByUserId.contains(projectId)){
                return true;
            }
            context.addMessageParameter(ORIGINAL_TEXT, "你没有该项目的权限");
            return false;
        } catch (Exception e){
            context.addMessageParameter(ORIGINAL_TEXT, "项目不存在");
            return false;
        }
    }
}
