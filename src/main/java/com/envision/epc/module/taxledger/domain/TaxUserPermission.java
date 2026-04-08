package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_user_permission")
public class TaxUserPermission extends AuditingEntity {
    private String userId;
    private String userName;
    private String employeeId;
    private PermissionLevelEnum permissionLevel;
    private String companyCode;
    private String grantedBy;
    private Integer isDeleted;
}
