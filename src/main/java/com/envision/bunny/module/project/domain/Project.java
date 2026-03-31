package com.envision.bunny.module.project.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.extract.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:05
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_projects")
public class Project extends AuditingEntity {
    /**
     * 项目名称
     */
    private String name;
    /**
     * 描述
     */
    private String description;
}
