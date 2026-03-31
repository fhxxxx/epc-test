package com.envision.bunny.module.permission.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.extract.infrastructure.mybatis.AuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author chaoyue.zhao1
 * @since 2025/08/11-10:05
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@TableName("t_project_permission")
public class Permission extends AuditingEntity {
    /**
     * 项目ID
     */
    @TableField("project_id")
    private Long projectId;
    /**
     * 用户工号
     */
    @TableField("user_code")
    private String userCode;
}
