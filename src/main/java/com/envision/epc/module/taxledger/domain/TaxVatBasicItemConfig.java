package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_vat_basic_item_config")
public class TaxVatBasicItemConfig extends AuditingEntity {
    private Integer itemSeq;
    private String companyCode;
    private String basicItem;
    private String isSplit;
    private String isDisplay;
    private Integer isDeleted;
}
