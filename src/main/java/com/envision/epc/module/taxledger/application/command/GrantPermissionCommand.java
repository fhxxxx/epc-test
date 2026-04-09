package com.envision.epc.module.taxledger.application.command;

import lombok.Data;

/**
 * 授权命令
 */
@Data
public class GrantPermissionCommand {
    /** 用户标识（建议传 sys_user.user_code） */
    private String userId;

    /** 用户名称（建议传 sys_user.username） */
    private String userName;

    /** 公司代码 */
    private String companyCode;
}
