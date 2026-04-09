package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 台账一次运行实例
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ledger_run")
public class TaxLedgerRun extends AuditingEntity {
    /** 关联台账主记录 ID */
    private Long ledgerId;

    /** 同一台账下的运行序号 */
    private Integer runNo;

    /** 触发类型 */
    private LedgerRunTriggerEnum triggerType;

    /** 运行模式快照（AUTO/GATED） */
    private LedgerRunModeEnum modeSnapshot;

    /** 运行状态 */
    private LedgerRunStatusEnum status;

    /** 当前批次 */
    private Integer currentBatch;

    /** 输入指纹 */
    private String inputFingerprint;

    /** 错误码 */
    private String errorCode;

    /** 错误信息 */
    private String errorMsg;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime endedAt;

    /** 逻辑删除标记：0-否，1-是 */
    private Integer isDeleted;
}
