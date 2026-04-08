package com.envision.epc.module.taxledger.application.command;

import com.envision.epc.module.taxledger.domain.PermissionLevelEnum;
import lombok.Data;

@Data
public class GrantPermissionCommand {
    private String userId;
    private String userName;
    private String employeeId;
    private PermissionLevelEnum permissionLevel;
    private String companyCode;
}
