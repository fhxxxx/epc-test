package com.envision.epc.module.extract.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @author wenjun.gu
 * @since 2025/8/12-16:13
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("t_ocr_tasks")
public class OcrTask extends AuditingEntity {
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
     * 文件id
     */
    private Long fileId;
    /**
     * OCR request id
     */
    private String requestId;
    /**
     * 顺序
     */
    private Integer position;
    /**
     * OCR页
     */
//    private String page;
    private Integer startPage;
    private Integer endPage;

    /**
     * OCR状态，例如 ocr_running, ocr_failed, success
     */
    private OcrTaskStatusEnum status;
    /**
     * ocr结果
     */
    private String result;
    /**
     * 错误信息
     */
    private String error;


    public void success(String result) {
        this.status = OcrTaskStatusEnum.SUCCESS;
        this.result = result;
    }

    public void running(String requestId) {
        this.status = OcrTaskStatusEnum.RUNNING;
        this.requestId = requestId;
    }
    public void running() {
        this.requestId = requestId;
    }

    public void failed(String error) {
        this.status = OcrTaskStatusEnum.FAILED;
        this.error = error;
    }
}
