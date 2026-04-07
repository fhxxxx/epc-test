package com.envision.epc.module.project.application;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.envision.epc.infrastructure.mybatis.BasicPagination;
import com.envision.epc.infrastructure.security.SecurityUtils;
import com.envision.epc.module.permission.application.PermissionConfig;
import com.envision.epc.module.permission.application.PermissionFacade;
import com.envision.epc.module.project.application.dtos.ProjectDTO;
import com.envision.epc.module.project.domain.Project;
import com.envision.epc.module.project.domain.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/08-16:23
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectQueryService {
    private final ProjectAssembler projectAssembler;
    private final ProjectRepository projectRepository;
    private final PermissionFacade permissionFacade;
    private final PermissionConfig permissionConfig;

    public BasicPagination<ProjectDTO> queryProject(Long lastId, Integer pageSize) {
        String userCode = SecurityUtils.getCurrentUserCode();
        List<Long> projectIds = new ArrayList<>();
        boolean isAdmin = permissionConfig.getAdmin().contains(userCode);
        if (!isAdmin){
            projectIds = permissionFacade.getProjectIdsByUserId(userCode);
        }
        Page<Project> projects = projectRepository.queryProject(pageSize, projectIds, isAdmin, lastId);
//        Page<Project> page = projectRepository.lambdaQuery()
//                .gt(lastId != null, Project::getId, lastId)
//                .orderByDesc(Project::getId)
//                .page(new Page<>(1, pageSize));

        return BasicPagination.of(projects, projectAssembler::toProjectDTO);
    }
}
