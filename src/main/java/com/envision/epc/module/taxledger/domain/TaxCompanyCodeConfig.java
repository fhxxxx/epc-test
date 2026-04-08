package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_company_code_config")
public class TaxCompanyCodeConfig extends AuditingEntity {
    private String companyCode;
    private String companyName;
    private String financeBpAd;
    private String financeBpName;
    private String financeBpEmail;
    private Integer isDeleted;
}
