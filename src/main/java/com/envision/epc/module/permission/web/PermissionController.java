package com.envision.epc.module.permission.web;

import com.envision.epc.module.permission.application.PermissionCommandService;
import com.envision.epc.module.permission.application.PermissionQueryService;
import com.envision.epc.module.permission.application.command.PermissionCommand;
import com.envision.epc.module.permission.application.dto.PermissionDto;
import com.envision.epc.module.validation.ValidAdminAndCreateBy;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:11
 */
@Validated
@RestController
@RequestMapping("/permission")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class PermissionController {
    private final PermissionCommandService commandService;
    private final PermissionQueryService queryService;

    /**
     * 获取项目权限列表
     * @param projectId 项目ID
     * @return 项目权限列表
     */
    @GetMapping("/{projectId}")
    public List<PermissionDto> getPermission(@PathVariable("projectId") Long projectId) {
        return queryService.getPermission(projectId);
    }

    /**
     * 修改项目权限
     * @param projectId 项目ID
     * @param permissionCommand 项目权限
     * @return 项目权限列表
     */
    @PutMapping("/{projectId}")
    public List<PermissionDto> alterPermission(@PathVariable("projectId") @ValidAdminAndCreateBy Long projectId, @RequestBody PermissionCommand permissionCommand) {
        return commandService.alterPermission(projectId, permissionCommand);
    }
}
