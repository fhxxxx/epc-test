package com.envision.epc.module.extract.application.dtos;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ContentStyle;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 发票提取结果导出DTO
 *
 * @author gangxiang.guan
 * @date 2025/10/10 17:09
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResultExportDTO {
    /**
     * 发票号
     */
    @ExcelProperty("发票号")
    @ContentStyle(dataFormat = 49)
    private String invoiceId;
    /**
     * 税额
     */
    @ExcelProperty("税额")
    @ContentStyle(dataFormat = 49)
    private String taxAmount;
    /**
     * 会计凭证号
     */
    @ExcelProperty("发票代码")
    @ContentStyle(dataFormat = 49)
    private String invoiceCode;
    /**
     * 销售方纳税人识别号
     */
    @ExcelProperty("销售方纳税人识别号")
    @ContentStyle(dataFormat = 49)
    private String vendorTaxId;
    /**
     * 销售方纳税人名称
     */
    @ExcelProperty("销售方纳税人名称")
    @ContentStyle(dataFormat = 49)
    private String vendorName;
    /**
     * 金额
     */
    @ExcelProperty("金额")
    @ContentStyle(dataFormat = 49)
    private String subTotal;

}
