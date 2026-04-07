package com.envision.epc.module.project.application;

import cn.hutool.core.text.CharSequenceUtil;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.event.ProjectDeleteEvent;
import com.envision.epc.module.project.application.command.AlterProjectCommand;
import com.envision.epc.module.project.application.command.ProjectCommand;
import com.envision.epc.module.project.application.dtos.ProjectDTO;
import com.envision.epc.module.project.domain.Project;
import com.envision.epc.module.project.domain.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/08-16:22
 */
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class ProjectCommandService {
    private final ProjectAssembler assembler;
    private final ProjectRepository repository;
    private final ApplicationEventPublisher publisher;


    public ProjectDTO addProject(ProjectCommand projectCommand) {
        String projectName = projectCommand.getName();
        if (CharSequenceUtil.isBlank(projectName)){
            throw new BizException(ErrorCode.BAD_REQUEST, "项目名称不能为空");
        }
        if (projectName.length() > 150){
            throw new BizException(ErrorCode.BAD_REQUEST, "项目名称不能超过150个字符");
        }
        Project project = assembler.toProject(projectCommand);
        repository.save(project);
        return assembler.toProjectDTO(project);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteProject(Long id){
        Optional<Project> optionalProject = repository.getOptById(id);
        if (optionalProject.isEmpty()){
            throw new BizException(ErrorCode.BAD_REQUEST, "项目不存在");
        }
        repository.removeById(id);
        publisher.publishEvent(new ProjectDeleteEvent(id));
    }

    public ProjectDTO alterProject(AlterProjectCommand projectCommand){
        String projectName = projectCommand.getName();
        if (CharSequenceUtil.isBlank(projectName)){
            throw new BizException(ErrorCode.BAD_REQUEST, "项目名称不能为空");
        }
        if (projectName.length() > 150){
            throw new BizException(ErrorCode.BAD_REQUEST, "项目名称不能超过150个字符");
        }
        Optional<Project> optionalProject = repository.getOptById(projectCommand.getId());
        if (optionalProject.isEmpty()){
            throw new BizException(ErrorCode.BAD_REQUEST, "项目不存在");
        }
        Project project = optionalProject.get();
        project.setName(projectCommand.getName());
        repository.updateById(project);
        return assembler.toProjectDTO(project);
    }
}
