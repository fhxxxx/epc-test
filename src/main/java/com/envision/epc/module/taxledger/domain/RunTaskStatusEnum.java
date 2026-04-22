package com.envision.epc.module.taxledger.domain;

/**
 * 节点任务状态
 */
public enum RunTaskStatusEnum {
    PENDING,
    RUNNING,
    BLOCKED_MANUAL,
    SUCCESS,
    FAILED,
    SKIPPED
}

