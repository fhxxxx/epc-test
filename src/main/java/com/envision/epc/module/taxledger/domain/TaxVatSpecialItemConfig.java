package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 增值税变动表特殊条目配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_vat_special_item_config")
public class TaxVatSpecialItemConfig extends AuditingEntity {
    /** 条目序号 */
    private Integer itemSeq;
    /** 公司代码 */
    private String companyCode;
    /** 特殊条目 */
    private String specialItem;
    /** 是否显示（Y/N） */
    private String isDisplay;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
