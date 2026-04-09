package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 运行阶段记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ledger_run_stage")
public class LedgerRunStage extends AuditingEntity {
    /** 运行 ID */
    private Long runId;

    /** 批次号 */
    private Integer batchNo;

    /** 阶段状态 */
    private LedgerRunStageStatusEnum status;

    /** 预期生成 sheet 数 */
    private Integer sheetCountTotal;

    /** 成功 sheet 数 */
    private Integer sheetCountSuccess;

    /** 依赖阶段 */
    private String dependsOn;

    /** 错误信息 */
    private String errorMsg;

    /** 确认用户 */
    private String confirmUser;

    /** 确认时间 */
    private LocalDateTime confirmTime;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 结束时间 */
    private LocalDateTime endedAt;

    /** 逻辑删除标记：0-否，1-是 */
    private Integer isDeleted;
}
