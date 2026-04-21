package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CumulativeTaxSummary23202355ColumnDTO {

    /** 所属期间（如202412、202501、202512） */
    private String period;

    /** 账面收入 */
    private BigDecimal bookIncome;
    /** 其他收益 */
    private BigDecimal otherIncome;
    /** 项目开票 */
    private BigDecimal projectInvoicing;
    /** 利息开票 */
    private BigDecimal interestInvoicing;

    /** 销项税额(A) */
    private BigDecimal outputTaxA;
    /** 本期进项税额(B) */
    private BigDecimal currentInputTaxB;
    /** 上期留抵税额(C) */
    private BigDecimal openingRetainedInputTaxC;
    /** 进项税额转出(D) */
    private BigDecimal inputTaxTransferOutD;
    /** 异地预缴税增值税(E) */
    private BigDecimal remotePrepaidVatE;
    /** 应纳增值税额(A-B-C+D-E) */
    private BigDecimal vatPayableAminusBminusCplusDminusE;
    /** 增值税 */
    private BigDecimal vatAmount;

    /** 印花税 */
    private BigDecimal stampDuty;
    /** 城建税 */
    private BigDecimal urbanConstructionTax;
    /** 教育费附加 */
    private BigDecimal educationSurcharge;
    /** 地方教育费附加 */
    private BigDecimal localEducationSurcharge;
    /** 房产税 */
    private BigDecimal propertyTax;
    /** 城镇土地使用 */
    private BigDecimal urbanLandUseTax;
    /** 企业所得税 */
    private BigDecimal corporateIncomeTax;
    /** 个税 */
    private BigDecimal individualIncomeTax;
    /** 残疾人保障金 */
    private BigDecimal disabledPersonsEmploymentSecurityFund;
    /** 合计 */
    private BigDecimal totalTaxAmount;
}
