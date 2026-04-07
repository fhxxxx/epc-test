package com.envision.epc.module.extract.domain;

import cn.hutool.core.text.CharSequenceUtil;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author wenjun.gu
 * @since 2025/9/2-20:06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_extract_run_version_files")
public class ExtractRunFile extends AuditingEntity {
    /**
     * 项目id
     */
    private Long projectId;
    /**
     * 运行记录
     */
    private Long extractRunId;
    /**
     * 版本
     */
    private Integer version;
    /**
     * 公司代码
     */
    private String companyCode;
    /**
     * 文件hash
     */
    private String hash;
    /**
     * 文件id
     */
    private Long fileId;
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 抽取状态
     */
    private ExtractRunStatusEnum status;
    /**
     * ocr结果
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
        this.error = error;
        this.status = ExtractRunStatusEnum.OCR_FAILED;

    }

    public void ocrRunning() {
        this.status = ExtractRunStatusEnum.OCR_RUNNING;
    }

    public void ocrSuccess() {
        this.status = ExtractRunStatusEnum.OCR_SUCCESS;
    }

    public void extractFailed(String message) {
        if (CharSequenceUtil.isNotBlank(error) && error.length() > 2000) {
            error = error.substring(0, 2000);
        }
        this.status = ExtractRunStatusEnum.EXTRACT_FAILED;
        this.error = message;
    }

    public void extractSuccess() {
        this.status = ExtractRunStatusEnum.SUCCESS;
    }
}
