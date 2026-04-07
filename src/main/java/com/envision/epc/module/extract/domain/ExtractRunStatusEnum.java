package com.envision.epc.module.extract.domain;

/**
 * @author wenjun.gu
 * @since 2025/9/1-16:05
 */
public enum ExtractRunStatusEnum {
    /**
     * 等待中
     */
    PENDING,
    /**
     * OCR运行中
     */
    OCR_RUNNING,
    /**
     * OCR成功
     */
    OCR_SUCCESS,
    /**
     * OCR失败
     */
    OCR_FAILED,
    /**
     *  提取中
     */
    EXTRACT_RUNNING,
    /**
     * 提取成功
     */
    SUCCESS,
    /**
     *  提取失败
     */
    EXTRACT_FAILED
}
