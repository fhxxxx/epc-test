package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 台账运行的批次阶段记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_ledger_run_stage")
public class TaxLedgerRunStage extends AuditingEntity {
    /** 运行ID */
    private Long runId;
    /** 批次号 */
    private Integer batchNo;
    /** 阶段状态 */
    private LedgerRunStageStatusEnum status;
    /** 应生成sheet数量 */
    private Integer sheetCountTotal;
    /** 成功sheet数量 */
    private Integer sheetCountSuccess;
    /** 依赖批次描述 */
    private String dependsOn;
    /** 阶段失败信息 */
    private String errorMsg;
    /** 人工确认人工号 */
    private String confirmUser;
    /** 人工确认时间 */
    private LocalDateTime confirmTime;
    /** 阶段开始时间 */
    private LocalDateTime startedAt;
    /** 阶段结束时间 */
    private LocalDateTime endedAt;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
