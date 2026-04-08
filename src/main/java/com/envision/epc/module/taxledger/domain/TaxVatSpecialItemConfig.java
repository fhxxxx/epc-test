package com.envision.epc.module.taxledger.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.envision.epc.infrastructure.mybatis.AuditingEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_tax_vat_special_item_config")
public class TaxVatSpecialItemConfig extends AuditingEntity {
    private Integer itemSeq;
    private String companyCode;
    private String specialItem;
    private String isDisplay;
    private Integer isDeleted;
}
