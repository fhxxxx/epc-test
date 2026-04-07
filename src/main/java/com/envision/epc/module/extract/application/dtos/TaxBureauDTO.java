package com.envision.epc.module.extract.application.dtos;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import lombok.Data;

/**
 * 税务局导入文件DTO
 *
 * @author gangxiang.guan
 * @date 2025/10/11 10:49
 */
@Data
public class TaxBureauDTO {

    /**
     * 序号
     */
    @ExcelProperty(value = "序号", index = 0)
    @ContentStyle(dataFormat = 49)
    private String index;
    /**
     * 是否勾选*
     */
    @ExcelProperty(value = "是否勾选*", index = 1)
    @ContentStyle(dataFormat = 49)
    private String isSelected;
    /**
     * 数电票号码
     */
    @ExcelProperty(value = "数电票号码", index = 2)
    @ContentStyle(dataFormat = 49)
    private String digitalInvoiceNumber;

    /**
     * 发票代码
     */
    @ExcelProperty(value = "发票代码", index = 3)
    @ContentStyle(dataFormat = 49)
    private String invoiceCode;

    /**
     * 发票号码
     */
    @ExcelProperty(value = "发票号码", index = 4)
    @ContentStyle(dataFormat = 49)
    private String invoiceNumber;

    /**
     * 开票日期*
     */
    @ExcelProperty(value = "开票日期*", index = 5)
    @ContentStyle(dataFormat = 49)
    private String invoiceDate;

    /**
     * 金额*
     */
    @ExcelProperty(value = "金额*", index = 6)
    @ContentStyle(dataFormat = 49)
    private String amount;

    /**
     * 票面税额*
     */
    @ExcelProperty(value = "票面税额*", index = 7)
    @ContentStyle(dataFormat = 49)
    private String faceTaxAmount;

    /**
     * 有效抵扣税额*
     */
    @ExcelProperty(value = "有效抵扣税额*", index = 8)
    @ContentStyle(dataFormat = 49)
    private String effectiveDeductibleTaxAmount;

    /**
     * 购买方识别号*
     */
    @ExcelProperty(value = "购买方识别号*", index = 9)
    @ContentStyle(dataFormat = 49)
    private String buyerTaxNumber;

    /**
     * 销售方纳税人名称
     */
    @ExcelProperty(value = "销售方纳税人名称", index = 10)
    @ContentStyle(dataFormat = 49)
    private String sellerName;

    /**
     * 销售方纳税人识别号
     */
    @ExcelProperty(value = "销售方纳税人识别号", index = 11)
    @ContentStyle(dataFormat = 49)
    private String sellerTaxNumber;

    /**
     * 发票来源
     */
    @ExcelProperty(value = "发票来源", index = 12)
    @ContentStyle(dataFormat = 49)
    private String invoiceSource;

    /**
     * 票种*
     */
    @ExcelProperty(value = "票种*", index = 13)
    @ContentStyle(dataFormat = 49)
    private String invoiceType;

    /**
     * 发票状态
     */
    @ExcelProperty(value = "发票状态", index = 14)
    @ContentStyle(dataFormat = 49)
    private String invoiceStatus;

    /**
     * 红字锁定标志
     */
    @ExcelProperty(value = "红字锁定标志", index = 15)
    @ContentStyle(dataFormat = 49)
    private String redMarkLockFlag;

    /**
     * 转内销证明编号
     */
    @ExcelProperty(value = "转内销证明编号", index = 16)
    @ContentStyle(dataFormat = 49)
    private String domesticSaleCertificateNumber;

    /**
     * 业务类型
     */
    @ExcelProperty(value = "业务类型", index = 17)
    @ContentStyle(dataFormat = 49)
    private String businessType;

    /**
     * 勾选时间
     */
    @ExcelProperty(value = "勾选时间", index = 18)
    @ContentStyle(dataFormat = 49)
    private String checkTime;

    /**
     * 发票风险等级
     */
    @ExcelProperty(value = "发票风险等级", index = 19)
    @ContentStyle(dataFormat = 49)
    private String invoiceRiskLevel;

    /**
     * 风险状态
     */
    @ExcelProperty(value = "风险状态", index = 20)
    @ContentStyle(dataFormat = 49)
    private String riskStatus;
}
