package com.envision.bunny.module.user.application.dto;

import com.envision.extract.module.user.domain.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * @author yakun.meng
 * @since 2024/5/9
 */
@Mapper(componentModel = "spring")
public abstract class UserAssembler {
    @Mappings({
            @Mapping(source = "user.username", target = "username"),
            @Mapping(source = "user.userCode", target = "userCode"),
            @Mapping(source = "user.account", target = "account"),
            @Mapping(source = "user.avatar", target = "avatar"),
            @Mapping(source = "user.searchStr", target = "searchStr"),
            @Mapping(source = "user.deptCode", target = "deptCode"),
            @Mapping(source = "user.deptName", target = "deptName"),
            @Mapping(source = "user.divisionCode", target = "divisionCode"),
            @Mapping(source = "user.divisionName", target = "divisionName"),
    })
    public abstract UserDto toDto(User user);

}
