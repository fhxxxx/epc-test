package com.envision.epc.module.taxledger.domain;

/**
 * 运行状态枚举
 */
public enum LedgerRunStatusEnum {
    PENDING,
    RUNNING,
    PAUSED,
    SUCCESS,
    FAILED,
    CANCELED,
    INVALIDATED
}
