package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MonthlyTaxSectionDTO {

    /** 第一行标题（例如：建安9%） */
    private String title;

    /** 成本 */
    private BigDecimal cost;

    /** 收入 */
    private BigDecimal income;

    /** 销项 */
    private BigDecimal outputTax;

    /** 已开票收入 */
    private BigDecimal invoicedIncome;

    /** 已开票税额 */
    private BigDecimal invoicedTaxAmount;

    /** 未开票收入 */
    private BigDecimal uninvoicedIncome;

    /** 未开票税额 */
    private BigDecimal uninvoicedTaxAmount;
}
