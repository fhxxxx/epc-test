package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_project_config")
public class TaxProjectConfig extends AuditingEntity {
    /** 公司代码 */
    private String companyCode;
    /** 税种 */
    private String taxType;
    /** 税目 */
    private String taxCategory;
    /** 项目名称 */
    private String projectName;
    /** 所属优惠期 */
    private String preferentialPeriod;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
