package com.envision.bunny.module.project.domain;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:32
 */
public interface ProjectRepository extends IService<Project> {
    Page<Project> queryProject(Integer pageSize, List<Long> ids, boolean isAdmin, Long lastId);
}
