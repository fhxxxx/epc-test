package com.envision.bunny.module.permission.application;

import com.envision.extract.module.permission.application.dto.PermissionDto;
import com.envision.extract.module.permission.domain.Permission;
import com.envision.extract.module.permission.domain.PermissionRepository;
import com.envision.extract.module.user.application.UserFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:13
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PermissionQueryService {
    private final PermissionRepository repository;
    private final PermissionAssembler assembler;
    private final UserFacade userFacade;

    public List<PermissionDto> getPermission(Long projectId){
        List<Permission> permissions = repository.getByProjectId(projectId);
        return permissions.stream()
                .map(permission -> assembler.toPermissionDto(permission, userFacade.getUserByUserCode(permission.getUserCode())))
                .toList();
    }
}
