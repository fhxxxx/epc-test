package com.envision.epc.module.project.web;

import com.envision.epc.infrastructure.mybatis.BasicPagination;
import com.envision.epc.module.project.application.ProjectCommandService;
import com.envision.epc.module.project.application.ProjectQueryService;
import com.envision.epc.module.project.application.command.AlterProjectCommand;
import com.envision.epc.module.project.application.command.ProjectCommand;
import com.envision.epc.module.project.application.dtos.ProjectDTO;
import com.envision.epc.module.validation.ValidAdminAndCreateBy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 项目
 *
 * @author chaoyue.zhao
 * @since 2025-08-07
 */
@Validated
@RestController
@RequestMapping("/project")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectController {
    private final ProjectCommandService projectCommandService;
    private final ProjectQueryService projectQueryService;

    /**
     * 创建项目（文件夹）
     * @param projectCommand 项目参数
     * @return 项目信息
     */
    @PostMapping
    public ProjectDTO addProject(@RequestBody ProjectCommand projectCommand){
        return projectCommandService.addProject(projectCommand);
    }

    /**
     * 查询项目列表
     * @param lastId 上次查询的id
     * @param pageSize 每页数量
     * @return 项目列表
     */
    @GetMapping
    public BasicPagination<ProjectDTO> queryProject(Long lastId, Integer pageSize){
        return projectQueryService.queryProject(lastId, pageSize);
    }

    /**
     * 删除项目
     * @param id 项目ID
     */
    @DeleteMapping("/{projectId}")
    public void deleteProject(@PathVariable("projectId") @ValidAdminAndCreateBy Long id){
        projectCommandService.deleteProject(id);
    }

    /**
     * 修改项目名称
     * @param id 项目id
     * @param projectCommand 修改项目参数
     * @return 修改结果
     */
    @PutMapping("/{projectId}")
    public ProjectDTO alterProject(@PathVariable("projectId") @ValidAdminAndCreateBy Long id, @RequestBody AlterProjectCommand projectCommand){
        projectCommand.setId(id);
        return projectCommandService.alterProject(projectCommand);
    }

}
