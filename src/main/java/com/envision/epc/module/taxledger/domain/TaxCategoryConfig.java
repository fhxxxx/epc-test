package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_category_config")
public class TaxCategoryConfig extends AuditingEntity {
    private String seqNo;
    private String companyCode;
    private String taxType;
    private String taxCategory;
    private String taxBasis;
    private BigDecimal collectionRatio;
    private BigDecimal taxRate;
    private String accountSubject;
    private Integer isDeleted;
}
