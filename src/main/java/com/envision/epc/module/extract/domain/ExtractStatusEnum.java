package com.envision.epc.module.extract.domain;

/**
 * @author wenjun.gu
 * @since 2025/8/13-15:23
 */
public enum ExtractStatusEnum {
    /**
     * OCR中
     */
    OCR_RUNNING,
    /**
     * 提取中
     */
    EXTRACT_RUNNING,
    /**
     * 成功
     */
    SUCCESS,
    /**
     * 失败
     */
    FAILED
}
