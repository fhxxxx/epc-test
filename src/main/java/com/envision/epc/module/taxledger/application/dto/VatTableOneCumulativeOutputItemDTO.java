package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VatTableOneCumulativeOutputItemDTO {

    /** 所属期（如202501、本台账期） */
    private String period;

    /** 涉税类别（如商品销售（13%）、工程建设（9%）、设计服务（6%）） */
    private String taxCategory;

    /** 已开票-不含税金额 */
    private BigDecimal invoicedAmountExclTax;

    /** 已开票-税额 */
    private BigDecimal invoicedTaxAmount;

    /** 已开票-税率 */
    private BigDecimal invoicedTaxRate;

    /** 未开票-不含税金额 */
    private BigDecimal uninvoicedAmountExclTax;

    /** 未开票-税额 */
    private BigDecimal uninvoicedTaxAmount;

    /** 未开票-税率 */
    private BigDecimal uninvoicedTaxRate;

    /** 合计-不含税金额 */
    private BigDecimal totalAmountExclTax;

    /** 合计-税额 */
    private BigDecimal totalTaxAmount;
}
