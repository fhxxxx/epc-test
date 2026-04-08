package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公司主数据
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_company")
public class TaxCompany extends AuditingEntity {
    /** 公司代码 */
    private String companyCode;
    /** 公司名称 */
    private String companyName;
    /** 财务BP AD账号 */
    private String financeBpAd;
    /** 财务BP姓名 */
    private String financeBpName;
    /** 财务BP邮箱 */
    private String financeBpEmail;
    /** 状态：1启用/0禁用 */
    private Integer status;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
