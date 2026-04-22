package com.envision.epc.module.taxledger.domain;

/**
 * 阶段状态枚举
 */
public enum LedgerRunStageStatusEnum {
    PENDING,
    RUNNING,
    BLOCKED_MANUAL,
    SUCCESS,
    FAILED,
    CONFIRMED,
    SKIPPED,
    INVALIDATED
}
