package com.envision.bunny.module.project.infrastructure;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.envision.extract.infrastructure.security.SecurityUtils;
import com.envision.extract.module.project.domain.Project;
import com.envision.extract.module.project.domain.ProjectRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:37
 */
@Service
public class ProjectRepositoryImpl extends ServiceImpl<ProjectMapper, Project> implements ProjectRepository {
    @Override
    public Page<Project> queryProject(Integer pageSize, List<Long> ids, boolean isAdmin, Long lastId) {
        String userCode = SecurityUtils.getCurrentUserCode();
        return this.lambdaQuery()
                .and(!isAdmin && CollUtil.isNotEmpty(ids),
                        q -> q.in(Project::getId, ids).or().eq(Project::getCreateBy, userCode))
                .and(!isAdmin && CollUtil.isEmpty(ids),
                        q -> q.eq(Project::getCreateBy, userCode))
                .and(lastId != null, q -> {
                    Project lastProject = this.getById(lastId);
                    if (lastProject != null) {
                        q.lt(Project::getCreateTime, lastProject.getCreateTime())
                                .or(wrapper -> wrapper.eq(Project::getCreateTime, lastProject.getCreateTime())
                                        .lt(Project::getId, lastId));
                    }
                })
                .orderByDesc(Project::getCreateTime)
                .orderByDesc(Project::getId)
                .page(new Page<>(1, pageSize));
    }
}
