package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TaxAccountingDifferenceMonitor23202355ItemDTO {

    /** 日期（如202501、202512） */
    private String period;

    /** 动态分类分组列表（每个表头框一个对象） */
    private List<CategoryIncomeItem> categoryIncomeList;

    /** 汇总-账面收入 */
    private BigDecimal totalBookIncome;

    /** 汇总-增值税申报收入 */
    private BigDecimal totalVatDeclaredIncome;

    /** 账税差异 */
    private BigDecimal accountingTaxDifference;

    /** 差异分析 */
    private String differenceAnalysis;

    @Data
    public static class CategoryIncomeItem {
        /** 分组标题（如“商品销售（13%）”“建筑安装（9%）”“设计服务（6%）”） */
        private String title;

        /** 账面收入 */
        private BigDecimal bookIncome;

        /** 申报收入 */
        private BigDecimal declaredIncome;
    }
}
