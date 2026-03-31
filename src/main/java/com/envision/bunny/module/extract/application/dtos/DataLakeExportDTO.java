package com.envision.bunny.module.extract.application.dtos;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author gangxiang.guan
 * @date 2025/10/10 17:09
 */
@Data
public class DataLakeExportDTO {
    /**
     * 发票号
     */
    @ExcelProperty("发票号")
    private String invoiceId;
    /**
     * 税额
     */
    @ExcelProperty("税额")
    private String taxAmount;
    /**
     * 会计凭证号
     */
    @ExcelProperty("会计凭证号")
    private String accountingDocumentNumber;
    /**
     * 公司代码
     */
    @ExcelProperty("公司代码")
    private String companyCode;
    /**
     * 会计年度/期间
     */
    @ExcelProperty("会计年度/期间")
    private String fiscalYearPeriod;
    /**
     * 行项目文本
     */
    @ExcelProperty("行文本")
    private String itemText;
    /**
     * 参照
     */
    @ExcelProperty("参照")
    private String reference;


}
