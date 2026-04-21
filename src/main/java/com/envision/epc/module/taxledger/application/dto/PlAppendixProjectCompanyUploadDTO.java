package com.envision.epc.module.taxledger.application.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlAppendixProjectCompanyUploadDTO {

    /** 拆分依据 */
    @ExcelProperty("拆分依据")
    private String splitBasis;

    /** 主营业务收入 */
    @ExcelProperty("主营业务收入")
    private BigDecimal mainBusinessRevenue;

    /** 销项 */
    @ExcelProperty("销项")
    private BigDecimal outputTax;
}
