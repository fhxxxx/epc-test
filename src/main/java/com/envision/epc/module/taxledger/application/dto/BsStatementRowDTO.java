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

    /** 期末余额 */
    @ExcelProperty("期末余额")
    private BigDecimal endingBalance;

    /** 年初余额 */
    @ExcelProperty("年初余额")
    private BigDecimal beginningBalance;
}

