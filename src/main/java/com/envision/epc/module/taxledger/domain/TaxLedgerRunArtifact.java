package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_ledger_run_artifact")
public class TaxLedgerRunArtifact extends AuditingEntity {
    private Long runId;
    private Integer batchNo;
    private LedgerArtifactTypeEnum artifactType;
    private String artifactName;
    private String blobPath;
    private String checksum;
    private Integer isDeleted;
}
