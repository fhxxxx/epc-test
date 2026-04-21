package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UninvoicedMonitorItemDTO {

    /** 所属期（如2025-1） */
    private String period;

    /** 申报账面数-营业收入 */
    private BigDecimal declaredMainBusinessRevenue;

    /** 申报账面数-利息收入 */
    private BigDecimal declaredInterestIncome;

    /** 申报账面数-其他收益 */
    private BigDecimal declaredOtherIncome;

    /** 申报账面数-销项税额 */
    private BigDecimal declaredOutputTax;

    /** 开票数-销售收入 */
    private BigDecimal invoicedSalesIncome;

    /** 开票数-销项税额 */
    private BigDecimal invoicedOutputTax;

    /** 未开票数-销售收入 */
    private BigDecimal uninvoicedSalesIncome;

    /** 未开票数-销项税额 */
    private BigDecimal uninvoicedOutputTax;
}
