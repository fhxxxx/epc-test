package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 台账任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ledger_job")
public class LedgerJob extends AuditingEntity {
    private String companyCode;
    @TableField("period_month")
    private String yearMonth;
    private LedgerJobStatusEnum status;
    private Long runId;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private Integer isDeleted;
}
