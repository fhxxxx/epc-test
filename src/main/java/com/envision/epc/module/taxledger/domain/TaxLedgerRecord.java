package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 台账主记录（公司+月份维度）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_ledger_record")
public class TaxLedgerRecord extends AuditingEntity {
    /** 公司代码 */
    private String companyCode;
    /** 账期（yyyy-MM） */
    private String yearMonth;
    /** 台账文件名 */
    private String ledgerName;
    /** 最终台账的Blob路径 */
    private String blobPath;
    /** 生成人工号 */
    private String generateUser;
    /** 生成状态 */
    private LedgerGenerateStatusEnum generateStatus;
    /** 状态描述/错误信息 */
    private String statusMsg;
    /** 最终生成时间 */
    private LocalDateTime generatedAt;
    /** 当前对外生效的最新运行ID */
    private Long latestRunId;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
