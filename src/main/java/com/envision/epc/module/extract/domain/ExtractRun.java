package com.envision.epc.module.extract.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wenjun.gu
 * @since 2025/8/12-14:55
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_extract_runs", autoResultMap = true)
public class ExtractRun extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
//    /**
//     * 文件id
//     */
//    @TableField(typeHandler = JacksonTypeHandler.class)
//    private List<Long> fileIds;
//    /**
//     * 提取配置
//     */
//    @TableField(typeHandler = JacksonTypeHandler.class)
//    private ExtractConfig extractConfig;
//    /**
//     * 抽取状态
//     */
//    private String status;
//    /**
//     * 错误信息
//     */
//    private String error;
    /**
     * 版本
     */
    private Integer currentVersion;
}
