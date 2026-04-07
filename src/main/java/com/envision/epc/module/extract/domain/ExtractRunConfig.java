package com.envision.epc.module.extract.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author wenjun.gu
 * @since 2025/9/1-09:56
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_extract_run_config", autoResultMap = true)
public class ExtractRunConfig extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 抽取记录id
     */
    private Long extractRunId;
    /**
     * 版本
     */
    private Integer version;
    /**
     * 抽取配置
     */
    private ExtractConfig extractConfig;
}
