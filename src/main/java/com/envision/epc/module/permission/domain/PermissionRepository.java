package com.envision.epc.module.permission.domain;

import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:09
 */
public interface PermissionRepository extends IService<Permission> {
    List<Permission> getByUserId(String userId);
    List<Permission> getByProjectId(Long projectId);
    void deleteByProjectId(Long projectId);
}
