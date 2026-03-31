package com.envision.bunny.module.permission.infrastructure;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.extract.module.permission.domain.Permission;
import com.envision.extract.module.permission.domain.PermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:09
 */
@Service
public class PermissionRepositoryImpl extends ServiceImpl<PermissionMapper, Permission> implements PermissionRepository {
    @Override
    public List<Permission> getByUserId(String userId) {
        return this.lambdaQuery()
                .eq(Permission::getUserCode, userId)
                .list();
    }

    @Override
    public List<Permission> getByProjectId(Long projectId) {
        return this.lambdaQuery()
                .eq(Permission::getProjectId, projectId)
                .orderByDesc(Permission::getCreateTime)
                .orderByDesc(Permission::getId)
                .list();
    }

    @Override
    public void deleteByProjectId(Long projectId) {
        this.lambdaUpdate()
                .eq(Permission::getProjectId, projectId)
                .remove();
    }
}
