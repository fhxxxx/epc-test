package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 台账运行产物记录（中间快照/最终文件）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_ledger_run_artifact")
public class TaxLedgerRunArtifact extends AuditingEntity {
    /** 运行ID */
    private Long runId;
    /** 所属批次 */
    private Integer batchNo;
    /** 产物类型 */
    private LedgerArtifactTypeEnum artifactType;
    /** 产物名称 */
    private String artifactName;
    /** Blob路径 */
    private String blobPath;
    /** 产物校验值 */
    private String checksum;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
