package com.envision.epc.module.permission.application;

import com.envision.epc.module.permission.application.dto.PermissionDto;
import com.envision.epc.module.permission.domain.Permission;
import com.envision.epc.module.user.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:48
 */
@Mapper(componentModel = "spring")
public abstract class PermissionAssembler {

    @Mappings({
            @Mapping(target = "id", source = "permission.id"),
            @Mapping(target = "projectId", source = "permission.projectId"),
            @Mapping(target = "userCode", source = "permission.userCode"),
            @Mapping(target = "userName", source = "user.username"),
            @Mapping(target = "account", source = "user.account"),
            @Mapping(target = "avatar", source = "user.avatar"),
    })
    public abstract PermissionDto toPermissionDto(Permission permission, User user);
}
