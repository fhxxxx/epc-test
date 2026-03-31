package com.envision.bunny.module.extract.domain;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.envision.extract.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * @author wenjun.gu
 * @since 2025/9/1-15:26
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "t_extract_run_versions", autoResultMap = true)
public class ExtractRunVersion extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 抽取运行ID
     */
    private Long extractRunId;
    /**
     * 版本
     */
    private Integer version;
    /**
     * 文件id
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Long> fileIds;
    /**
     * 提取配置
     */
    @TableField(value = "extract_config", typeHandler = JacksonTypeHandler.class)
    private ExtractConfig extractConfig;
    /**
     * 抽取状态
     */
    private ExtractRunStatusEnum status;
    /**
     * 合并后的OCR结果路径
     */
    private String ocrResult;
    /**
     * 错误信息
     */
    private String error;
    public void ocrFailed(String error) {
        if (CharSequenceUtil.isNotBlank(error) && error.length() > 2000) {
            error = error.substring(0, 2000);
        }
        this.status = ExtractRunStatusEnum.OCR_FAILED;
        this.error = error;
    }

    public void ocrSuccess(String ocrResult) {
        this.ocrResult = ocrResult;
        this.status = ExtractRunStatusEnum.OCR_SUCCESS;
    }

    public void ocrRunning() {
        this.status = ExtractRunStatusEnum.OCR_RUNNING;
    }

    public void extractRunning() {
        this.status = ExtractRunStatusEnum.EXTRACT_RUNNING;
    }

    public void extractFailed(String error) {
        if (CharSequenceUtil.isNotBlank(error) && error.length() > 2000) {
            error = error.substring(0, 2000);
        }
        this.error = error;
        this.status = ExtractRunStatusEnum.EXTRACT_FAILED;
    }

    public void extractSuccess() {
        this.status = ExtractRunStatusEnum.SUCCESS;
    }
}
