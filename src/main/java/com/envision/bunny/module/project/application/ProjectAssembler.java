package com.envision.bunny.module.project.application;

import com.envision.extract.module.project.application.command.ProjectCommand;
import com.envision.extract.module.project.application.dtos.ProjectDTO;
import com.envision.extract.module.project.domain.Project;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/08-16:29
 */
@Mapper(componentModel = "spring")
public abstract class ProjectAssembler {

    @Mapping(target = "name", source = "projectCommand.name")
    public abstract Project toProject(ProjectCommand projectCommand);

    @Mappings({
            @Mapping(target = "id", source = "project.id"),
            @Mapping(target = "name", source = "project.name"),
            @Mapping(target = "createTime", source = "project.createTime"),
            @Mapping(target = "createBy", source = "project.createBy"),
            @Mapping(target = "createByName", source = "project.createByName")
    })
    public abstract ProjectDTO toProjectDTO(Project project);
}
