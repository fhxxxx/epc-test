package com.envision.epc.module.taxledger.application.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
/**
 * 印花税解析汇总行 DTO（解析层对象）。
 * <p>来源：{@code StampTaxSheetParser}，用于解析 FileCategoryEnum.STAMP_TAX 文件中“汇总”区域表格。</p>
 * <p>用途：作为 Summary 构建阶段的输入数据之一，后续会映射为 {@code SummarySheetDTO.StampDutyItem}。</p>
 */
public class StampDutySummaryRowDTO {

    /** 合同类别 */
    @ExcelProperty("合同类别")
    private String contractCategory;

    /** 应税金额 */
    @ExcelProperty("应税金额")
    private BigDecimal taxableAmount;

    /** 税率 */
    @ExcelProperty("税率")
    private BigDecimal taxRate;

    /** 应纳税额 */
    @ExcelProperty("应纳税额")
    private BigDecimal taxPayableAmount;
}
