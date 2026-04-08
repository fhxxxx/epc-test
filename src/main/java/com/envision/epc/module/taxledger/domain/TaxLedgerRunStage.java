package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_ledger_run_stage")
public class TaxLedgerRunStage extends AuditingEntity {
    private Long runId;
    private Integer batchNo;
    private LedgerRunStageStatusEnum status;
    private Integer sheetCountTotal;
    private Integer sheetCountSuccess;
    private String dependsOn;
    private String errorMsg;
    private String confirmUser;
    private LocalDateTime confirmTime;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer isDeleted;
}
