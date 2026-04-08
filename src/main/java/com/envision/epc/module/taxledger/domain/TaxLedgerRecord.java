package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_ledger_record")
public class TaxLedgerRecord extends AuditingEntity {
    private String companyCode;
    private String yearMonth;
    private String ledgerName;
    private String blobPath;
    private String generateUser;
    private LedgerGenerateStatusEnum generateStatus;
    private String statusMsg;
    private LocalDateTime generatedAt;
    private Long latestRunId;
    private Integer isDeleted;
}
