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
@TableName("t_tax_ledger_run")
public class TaxLedgerRun extends AuditingEntity {
    /** 关联台账主记录ID */
    private Long ledgerId;
    /** 同一台账下的运行序号 */
    private Integer runNo;
    /** 触发类型（手动/重试/续跑） */
    private LedgerRunTriggerEnum triggerType;
    /** 本次运行模式快照（AUTO/GATED） */
    private LedgerRunModeEnum modeSnapshot;
    /** 运行状态 */
    private LedgerRunStatusEnum status;
    /** 当前批次 */
    private Integer currentBatch;
    /** 输入快照指纹 */
    private String inputFingerprint;
    /** 模板编码 */
    private String templateCode;
    /** 模板版本 */
    private String templateVersion;
    /** 模板校验值 */
    private String templateChecksum;
    /** 错误编码 */
    private String errorCode;
    /** 错误信息 */
    private String errorMsg;
    /** 开始时间 */
    private LocalDateTime startedAt;
    /** 结束时间 */
    private LocalDateTime endedAt;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
