package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ContractStampDutyLedgerItemDTO {

    /** 序号 */
    private String serialNo;

    /** 季度 */
    private String quarter;

    /** 单号/合同编码 */
    private String contractNoOrCode;

    /** 供应商 */
    private String supplier;

    /** 合同内容 */
    private String contractContent;

    /** 合同金额（含税） */
    private BigDecimal contractAmountInclTax;

    /** 税率 */
    private BigDecimal taxRate;

    /** 合同金额 */
    private BigDecimal contractAmount;

    /** 印花税税目 */
    private String stampDutyTaxItem;

    /** 印花税税率 */
    private BigDecimal stampDutyTaxRate;

    /** 应纳税额（元） */
    private BigDecimal taxableAmount;

    /** 优惠比例 */
    private BigDecimal preferentialRatio;

    /** 实缴税额（元） */
    private BigDecimal actualTaxPaid;

    /** 申报日期 */
    private String declarationDate;
}
