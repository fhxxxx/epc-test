package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 台账主记录（公司+月份）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_ledger_record")
public class LedgerRecord extends AuditingEntity {
    /** 公司代码 */
    private String companyCode;

    /** 账期（yyyy-MM） */
    @TableField("`year_month`")
    private String yearMonth;

    /** 台账文件名 */
    private String ledgerName;

    /** 最终台账 Blob 路径 */
    private String blobPath;

    /** 生成人工号 */
    private String generateUser;

    /** 生成状态 */
    private LedgerGenerateStatusEnum generateStatus;

    /** 状态描述 */
    private String statusMsg;

    /** 生成时间 */
    private LocalDateTime generatedAt;

    /** 逻辑删除标记：0-否，1-是 */
    private Integer isDeleted;
}
