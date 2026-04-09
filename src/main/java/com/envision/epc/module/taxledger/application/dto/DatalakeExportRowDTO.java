package com.envision.epc.module.taxledger.application.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * Datalake export row with a single header row.
 */
@Data
public class DatalakeExportRowDTO {
    @ExcelProperty(value = "公司代码", index = 0)
    private String companyCode;

    @ExcelProperty(value = "财政年/阶段", index = 1)
    private String fiscalYearPeriod;

    @ExcelProperty(value = "凭证日期", index = 2)
    private String documentDate;

    @ExcelProperty(value = "过账日期", index = 3)
    private String postingDateInTheDocument;

    @ExcelProperty(value = "凭证类型", index = 4)
    private String documentType;

    @ExcelProperty(value = "凭证编号", index = 5)
    private String documentPrintNumber;

    @ExcelProperty(value = "抬头文本", index = 6)
    private String documentHeaderText;

    @ExcelProperty(value = "行项目号", index = 7)
    private String numberOfLineItemWithinAccountingDocument;

    @ExcelProperty(value = "总账科目", index = 8)
    private String account;

    @ExcelProperty(value = "本币金额", index = 9)
    private String localAmount;

    @ExcelProperty(value = "本币", index = 10)
    private String currencyKeyOfTheLocalCurrency;

    @ExcelProperty(value = "货币金额", index = 11)
    private String documentAmount;

    @ExcelProperty(value = "凭证货币", index = 12)
    private String currencyKeyOfTheDocumentCurrency;

    @ExcelProperty(value = "行文本", index = 13)
    private String itemText;

    @ExcelProperty(value = "付款参考", index = 14)
    private String paymentReference;

    @ExcelProperty(value = "贸易伙伴", index = 15)
    private String companyIdOfTradingPartner;

    @ExcelProperty(value = "客户", index = 16)
    private String customerNumber;

    @ExcelProperty(value = "供应商", index = 17)
    private String vendorNumber;

    @ExcelProperty(value = "成本中心", index = 18)
    private String costCenter;

    @ExcelProperty(value = "WBS", index = 19)
    private String wbsElement;

    @ExcelProperty(value = "参照", index = 20)
    private String reference;

    @ExcelProperty(value = "冲销凭证号", index = 21)
    private String reverseDocumentNumber;

    @ExcelProperty(value = "采购凭证", index = 22)
    private String purchasingDocumentNumber;

    @ExcelProperty(value = "参考代码1", index = 23)
    private String referenceKey1ForLineItem;

    @ExcelProperty(value = "分配", index = 24)
    private String assignment;

    @ExcelProperty(value = "用户名", index = 25)
    private String userName;
}
