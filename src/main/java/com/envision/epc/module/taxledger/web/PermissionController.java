package com.envision.epc.module.taxledger.web;

import com.envision.epc.module.taxledger.application.command.GrantPermissionCommand;
import com.envision.epc.module.taxledger.application.service.PermissionService;
import com.envision.epc.module.taxledger.domain.UserPermission;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 权限管理接口
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/tax-ledger/permissions")
public class PermissionController {
    private final PermissionService permissionService;

    /**
     * 查询权限列表
     */
    @GetMapping
    public List<UserPermission> list(@RequestParam(required = false) String companyCode) {
        return permissionService.listByCompany(companyCode);
    }

    /**
     * 授权
     */
    @PostMapping
    public UserPermission grant(@RequestBody GrantPermissionCommand command) {
        return permissionService.grant(command);
    }

    /**
     * 批量授权
     */
    @PostMapping("/batch")
    public List<UserPermission> grantBatch(@RequestBody List<GrantPermissionCommand> commands) {
        return permissionService.grantBatch(commands);
    }

    /**
     * 撤销授权
     */
    @DeleteMapping
    public void revoke(@RequestParam(required = false) String userId,
                       @RequestParam(required = false) String employeeId,
                       @RequestParam String companyCode) {
        String finalUserId = StringUtils.hasText(userId) ? userId : employeeId;
        permissionService.revoke(finalUserId, companyCode);
    }
}
