package com.envision.epc.module.taxledger.domain;

/**
 * 台账任务状态
 */
public enum LedgerJobStatusEnum {
    PENDING,
    VALIDATING,
    VALIDATION_FAILED,
    GENERATING,
    SUCCESS,
    FAILED
}
