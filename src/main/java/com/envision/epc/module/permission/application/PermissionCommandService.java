package com.envision.epc.module.permission.application;

import com.envision.epc.module.event.ProjectDeleteEvent;
import com.envision.epc.module.permission.application.command.PermissionCommand;
import com.envision.epc.module.permission.application.dto.PermissionDto;
import com.envision.epc.module.permission.domain.Permission;
import com.envision.epc.module.permission.domain.PermissionRepository;
import com.envision.epc.module.user.application.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:12
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@EnableConfigurationProperties(PermissionConfig.class)
public class PermissionCommandService {
    private final PermissionRepository repository;
    private final PermissionAssembler assembler;
    private final UserFacade userFacade;
    private final PermissionConfig permissionConfig;

//    @Transactional(rollbackFor = Exception.class)
//    public void deleteByProjectId(ProjectDeleteEvent event) {
//        List<Permission> permissions = repository.getByProjectId(event.getProjectId());
//        if (CollUtil.isNotEmpty( permissions)){
//            repository.removeBatchByIds( permissions);
//        }
//    }

    @Transactional(rollbackFor = Exception.class)
    public List<PermissionDto> alterPermission(Long projectId, PermissionCommand permissionCommand) {
        repository.deleteByProjectId(projectId);
        List<String> userCodes = permissionCommand.getUserCode();
        List<Permission> permissions = userCodes.stream()
                .distinct()
                .map(decode -> new Permission(projectId, decode))
                .toList();
        repository.saveBatch(permissions);
        return permissions.stream()
                .map(permission -> assembler.toPermissionDto(permission, userFacade.getUserByUserCode(permission.getUserCode())))
                .toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteByProjectId(ProjectDeleteEvent event) {
        Long projectId = event.getProjectId();
        repository.lambdaUpdate()
                .eq(Permission::getProjectId, projectId)
                .remove();
    }
}
