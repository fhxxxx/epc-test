package com.envision.bunny.module.extract.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.envision.extract.infrastructure.mybatis.AuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 提取结果表
 *
 * @author gangxiang.guan
 * @since 2025/9/22-11:06
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@TableName(value = "t_extract_task_results", autoResultMap = true)
public class ExtractTaskResult extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * t_extract_run表的关联id
     */
    private Long extractRunId;
    /**
     * t_extract_tasks表的关联id
     */
    private Long extractTaskId;
    /**
     * 版本
     */
    private Integer version;
    /**
     * 字段类型(单个字段，组合字段)
     */
    private ParameterTypeEnum type;
    /**
     * 组合名称(仅当字段类型为组合字段时有效)
     */
    private String compositeName;
    /**
     * 组合index(仅当字段类型为组合字段时有效)
     */
    private Long compositeIndex;
    /**
     * 字段名称
     */
    private String primitiveName;
    /**
     * 提取内容
     */
    private String content;
    /**
     * 位置信息json
     */
    @TableField(value = "polygons", typeHandler = JacksonTypeHandler.class)
    private List<Polygon> polygons;
    /**
     * 段落范围
     */
    private String paraRange;

    public ExtractTaskResult(String content, Long extractTaskId) {
        this.content = content;
        this.extractTaskId = extractTaskId;
    }

    public ExtractTaskResult(ParameterTypeEnum type, String primitiveName, String content, List<Polygon> polygons) {
        this.type = type;
        this.primitiveName = primitiveName;
        this.content = content;
        this.polygons = polygons;
    }
}
