package com.envision.epc.module.extract.domain;

/**
 * @author wenjun.gu
 * @since 2025/9/5-11:50
 */
public enum OcrTaskStatusEnum {
    /**
     * 等待中
     */
    PENDING,
    /**
     * OCR运行中
     */
    RUNNING,
    /**
     * 提取成功
     */
    SUCCESS,
    /**
     *  提取失败
     */
    FAILED
}
