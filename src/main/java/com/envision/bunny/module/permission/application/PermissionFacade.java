package com.envision.bunny.module.permission.application;

import com.envision.extract.module.permission.domain.Permission;
import com.envision.extract.module.permission.domain.PermissionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:12
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PermissionFacade {
    private final PermissionRepository repository;

    public List<Long> getProjectIdsByUserId(String userId){
        List<Permission> projectIdsByUserId = repository.getByUserId(userId);
        return projectIdsByUserId.stream().map(Permission::getProjectId).toList();
    }
}
