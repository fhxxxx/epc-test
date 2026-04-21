package com.envision.epc.module.taxledger.application.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class VatInputCertificationItemDTO {

    /** 序号 */
    @ExcelProperty("序号")
    private String serialNo;

    /** 勾选状态 */
    @ExcelProperty("勾选状态")
    private String selectionStatus;

    /** 发票来源 */
    @ExcelProperty("发票来源")
    private String invoiceSource;

    /** 转内销证明编号 */
    @ExcelProperty("转内销证明编号")
    private String transferToDomesticProofNo;

    /** 数电票号码 */
    @ExcelProperty("数电票号码")
    private String digitalInvoiceNo;

    /** 发票代码 */
    @ExcelProperty("发票代码")
    private String invoiceCode;

    /** 发票号码 */
    @ExcelProperty("发票号码")
    private String invoiceNo;

    /** 开票日期 */
    @ExcelProperty("开票日期")
    private String invoiceDate;

    /** 销售方纳税人识别号 */
    @ExcelProperty("销售方纳税人识别号")
    private String sellerTaxpayerId;

    /** 销售方纳税人名称 */
    @ExcelProperty("销售方纳税人名称")
    private String sellerTaxpayerName;

    /** 金额 */
    @ExcelProperty("金额")
    private BigDecimal amount;

    /** 税额 */
    @ExcelProperty("税额")
    private BigDecimal taxAmount;

    /** 有效抵扣税额 */
    @ExcelProperty("有效抵扣税额")
    private BigDecimal effectiveDeductibleTaxAmount;

    /** 票种 */
    @ExcelProperty("票种")
    private String invoiceType;

    /** 票种标签 */
    @ExcelProperty("票种标签")
    private String invoiceTypeTag;

    /** 发票状态 */
    @ExcelProperty("发票状态")
    private String invoiceStatus;

    /** 勾选时间 */
    @ExcelProperty("勾选时间")
    private String selectionTime;

    /** 发票风险等级 */
    @ExcelProperty("发票风险等级")
    private String invoiceRiskLevel;

    /** 风险状态 */
    @ExcelProperty("风险状态")
    private String riskStatus;
}
