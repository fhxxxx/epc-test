package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 台账运行节点任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ledger_run_task")
public class LedgerRunTask extends AuditingEntity {
    private Long runId;
    private String nodeCode;
    private Integer batchNo;
    private RunTaskStatusEnum status;
    private String dependsOn;
    private ManualActionTypeEnum manualActionType;
    private String inputRefs;
    private String outputBlobPath;
    private String errorMsg;
    private Integer retryCount;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer isDeleted;
}

