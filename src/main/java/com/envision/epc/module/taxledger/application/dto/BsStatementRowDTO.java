package com.envision.epc.module.taxledger.application.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class BsStatementRowDTO {

    /** 项目 */
    @ExcelProperty("项目")
    private String itemName;

    /** 行 */
    @ExcelProperty("行")
    private String lineNo;

    /** 年初数 */
    @ExcelProperty("年初数")
    private BigDecimal yearStartAmount;

    /** 累计发生数 */
    @ExcelProperty("累计发生数")
    private BigDecimal accumulatedAmount;
}
