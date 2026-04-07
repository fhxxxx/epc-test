package com.envision.epc.module.extract.application.dtos;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:54
 */
@Getter
@Setter
@ToString
public class OcrTaskDTO {
    /**
     * 文件id
     */
    private Long fileId;
    /**
     * 顺序
     */
    private Integer position;
    /**
     * OCR页
     */
    private String page;
    /**
     * OCR状态，例如 ocr_running, ocr_failed, success
     */
    private String status;
    /**
     * ocr结果
     */
    private String result;
    /**
     * 错误信息
     */
    private String error;
}
