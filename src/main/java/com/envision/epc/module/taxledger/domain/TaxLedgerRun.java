package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_ledger_run")
public class TaxLedgerRun extends AuditingEntity {
    private Long ledgerId;
    private Integer runNo;
    private LedgerRunTriggerEnum triggerType;
    private LedgerRunModeEnum modeSnapshot;
    private LedgerRunStatusEnum status;
    private Integer currentBatch;
    private String inputFingerprint;
    private String templateCode;
    private String templateVersion;
    private String templateChecksum;
    private String errorCode;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer isDeleted;
}
