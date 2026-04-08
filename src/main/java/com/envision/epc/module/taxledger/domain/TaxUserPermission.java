package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户权限记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_user_permission")
public class TaxUserPermission extends AuditingEntity {
    /** 用户ID */
    private String userId;
    /** 用户姓名 */
    private String userName;
    /** 工号 */
    private String employeeId;
    /** 权限级别 */
    private PermissionLevelEnum permissionLevel;
    /** 公司代码（超级管理员为空） */
    private String companyCode;
    /** 授权人工号 */
    private String grantedBy;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
