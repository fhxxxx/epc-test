package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_project_config")
public class TaxProjectConfig extends AuditingEntity {
    private String companyCode;
    private String taxType;
    private String taxCategory;
    private String projectName;
    private String preferentialPeriod;
    private Integer isDeleted;
}
