package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 增值税变动表基础条目配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_vat_basic_item_config")
public class TaxVatBasicItemConfig extends AuditingEntity {
    /** 条目序号 */
    private Integer itemSeq;
    /** 公司代码（可为空） */
    private String companyCode;
    /** 基础条目 */
    private String basicItem;
    /** 是否拆分（Y/N） */
    private String isSplit;
    /** 是否显示（Y/N） */
    private String isDisplay;
    /** 逻辑删除标记：0否/1是 */
    private Integer isDeleted;
}
