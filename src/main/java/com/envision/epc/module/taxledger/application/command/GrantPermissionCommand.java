package com.envision.epc.module.taxledger.application.command;

import com.envision.epc.module.taxledger.domain.PermissionLevelEnum;
import lombok.Data;

/**
 * 授权命令
 */
@Data
public class GrantPermissionCommand {
    /**
     * 用户ID（统一用户标识）
     */
    private String userId;
    /**
     * 用户名称
     */
    private String userName;
    /**
     * 工号
     */
    private String employeeId;
    /**
     * 权限级别
     */
    private PermissionLevelEnum permissionLevel;
    /**
     * 公司代码（超级管理员可为空）
     */
    private String companyCode;
}
