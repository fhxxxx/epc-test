package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 税目配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_category_config")
public class TaxCategoryConfig extends AuditingEntity {
    /** 序号 */
    private String seqNo;
    /** 公司代码（可为空，表示通用） */
    private String companyCode;
    /** 税种 */
    private String taxType;
    /** 税目 */
    private String taxCategory;
    /** 计税依据 */
    private String taxBasis;
    /** 征收比例 */
    private BigDecimal collectionRatio;
    /** 税率 */
    private BigDecimal taxRate;
    /** 会计科目 */
    private String accountSubject;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
