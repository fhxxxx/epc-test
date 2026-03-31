package com.envision.bunny.module.extract.application.dtos;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ContentFontStyle;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发票提取结果与数据湖捞取结果对比后的结果DTO
 *
 * @author gangxiang.guan
 * @date 2025/10/10 17:09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@ContentFontStyle
public class ResultDataLakeCompareDTO {

    public final static String MATCHED = "是";
    public final static String UNMATCHED = "否";

    /***********数据湖字段************/
    /**
     * 发票号
     */
    @ExcelProperty(value = {"会计凭证", "发票号"}, index = 0)
    @ContentStyle(dataFormat = 49)
    private String invoiceId;
    /**
     * 税额
     */
    @ExcelProperty(value = {"会计凭证", "税额"}, index = 1)
    @ContentStyle(dataFormat = 49)
    private String taxAmount;
    /**
     * 会计凭证号
     */
    @ExcelProperty(value = {"会计凭证", "会计凭证号"}, index = 2)
    @ContentStyle(dataFormat = 49)
    private String accountingDocumentNumber;
    /**
     * 公司代码
     */
    @ExcelProperty(value = {"会计凭证", "公司代码"}, index = 3)
    @ContentStyle(dataFormat = 49)
    private String companyCode;
    /**
     * 会计年度/期间
     */
    @ExcelProperty(value = {"会计凭证", "会计年度/期间"}, index = 4)
    @ContentStyle(dataFormat = 49)
    private String fiscalYearPeriod;
    /**
     * 行项目文本
     */
    @ExcelProperty(value = {"会计凭证", "行文本"}, index = 5)
    @ContentStyle(dataFormat = 49)
    private String itemText;
    /**
     * 参照
     */
    @ExcelProperty(value = {"会计凭证", "参照"}, index = 6)
    @ContentStyle(dataFormat = 49)
    private String reference;

    /************发票提取结果字段*************/
    /**
     * 发票号
     */
    @ExcelProperty(value = {"发票PDF", "发票号"}, index = 7)
    @ContentStyle(dataFormat = 49)
    private String RInvoiceId;
    /**
     * 税额
     */
    @ExcelProperty(value = {"发票PDF", "税额"}, index = 8)
    @ContentStyle(dataFormat = 49)
    private String RTaxAmount;
    /**
     * 会计凭证号
     */
    @ExcelProperty(value = {"发票PDF", "发票代码"}, index = 9)
    @ContentStyle(dataFormat = 49)
    private String RInvoiceCode;
    /**
     * 销售方纳税人识别号
     */
    @ExcelProperty(value = {"发票PDF", "销售方纳税人识别号"}, index = 10)
    @ContentStyle(dataFormat = 49)
    private String RVendorTaxId;
    /**
     * 销售方纳税人名称
     */
    @ExcelProperty(value = {"发票PDF", "销售方纳税人名称"}, index = 11)
    @ContentStyle(dataFormat = 49)
    private String RVendorName;
    /**
     * 金额
     */
    @ExcelProperty(value = {"发票PDF", "金额"}, index = 12)
    @ContentStyle(dataFormat = 49)
    private String RSubTotal;

    @ExcelProperty(value = {"", "是否匹配成功"}, index = 13)
    @ContentStyle(dataFormat = 49)
    private String matchSuccess;


    public ResultDataLakeCompareDTO(String invoiceId, String taxAmount, String RTaxAmount, String RSubTotal) {
        this.invoiceId = invoiceId;
        this.taxAmount = taxAmount;
        this.RTaxAmount = RTaxAmount;
        this.RSubTotal = RSubTotal;
    }

    public static ResultDataLakeCompareDTO getSumRow(String sumTaxAmount, String sumRTaxAmount, String sumSubTotal) {
        return new ResultDataLakeCompareDTO("合计：", sumTaxAmount, sumRTaxAmount, sumSubTotal);
    }
}
