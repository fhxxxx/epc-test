package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
/**
 * Summary 页渲染数据对象。
 * <p>对应台账 Sheet：Summary；用于承载 Summary 页四大分段与总计区的最终展示数据。</p>
 */
public class SummarySheetDTO {

    /** 公司名称（表头） */
    private String companyName;

    /** 台账期间（如2025-02） */
    private String ledgerPeriod;

    /** 印花税分段 */
    private List<StampDutyItem> stampDutyRows;

    /** 增值税分段（从原commonTaxRows中拆分） */
    private List<CommonTaxItem> vatTaxRows;

    /** 公共税种分段（非增值税，除企业所得税） */
    private List<CommonTaxItem> commonTaxRows;

    /** 企业所得税分段 */
    private List<CorporateIncomeTaxItem> corporateIncomeTaxRows;

    /** 全表最终合计 */
    private FinalTotalItem finalTotal;

    @Data
    /**
     * Summary 页-印花税分段行对象。
     */
    public static class StampDutyItem {
        private Integer seqNo;
        private String taxType;
        private String taxItem;
        private String taxBasisDesc;
        private BigDecimal taxBaseQuarter;
        private BigDecimal levyRatio;
        private BigDecimal taxRate;
        private BigDecimal actualTaxPayable;
        private BigDecimal taxBaseMonth1;
        private BigDecimal taxBaseMonth2;
        private BigDecimal taxBaseMonth3;
        private String varianceReason;
    }

    @Data
    /**
     * Summary 页-公共税种/增值税分段行对象。
     */
    public static class CommonTaxItem {
        private Integer seqNo;
        private String taxType;
        private String taxItem;
        private String taxBasisDesc;
        private BigDecimal taxBaseAmount;
        private BigDecimal levyRatio;
        private BigDecimal taxRate;
        private BigDecimal actualTaxPayable;
        private String accountCode;
        private BigDecimal bookAmount;
        private BigDecimal varianceAmount;
        private String varianceReason;
    }

    @Data
    /**
     * Summary 页-企业所得税分段行对象。
     */
    public static class CorporateIncomeTaxItem {
        private String seqNo;
        private String projectName;
        private String preferentialPeriod;
        private BigDecimal taxableIncome;
        private BigDecimal taxRate;
        private BigDecimal annualTaxPayable;
        /** 固定映射Q1（一季度） */
        private BigDecimal q1Tax;
        /** 固定映射Q2（二季度） */
        private BigDecimal q2Tax;
        /** 固定映射Q3（三季度） */
        private BigDecimal q3Tax;
        /** 固定映射Q4（四季度） */
        private BigDecimal q4Tax;
        private BigDecimal q1PayLastYearQ4;
        private BigDecimal lossCarryforwardUsed;
        private BigDecimal remainingLossCarryforward;
    }

    @Data
    /**
     * Summary 页-全表总计对象。
     */
    public static class FinalTotalItem {
        private String totalTitle;
        /** 兼容保留：渲染时不使用，最终合计由公式生成 */
        private BigDecimal declaredTotal;
        /** 兼容保留：渲染时不使用，最终合计由公式生成 */
        private BigDecimal bookTotal;
    }
}
