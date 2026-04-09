package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户公司权限映射
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_user_permission")
public class TaxUserPermission extends AuditingEntity {
    /** 用户标识（对应 sys_user.user_code） */
    private String userId;

    /** 用户名称（对应 sys_user.username） */
    private String userName;

    /** 公司代码 */
    private String companyCode;

    /** 逻辑删除标记：0-否，1-是 */
    private Integer isDeleted;
}
