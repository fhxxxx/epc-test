package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectCumulativePaymentSheetDTO {

    /** 全表行数据 */
    private List<ProjectCumulativePaymentRowDTO> rows;

    @Data
    public static class ProjectCumulativePaymentRowDTO {
        /** 实际缴纳所属期（如2025-09） */
        private String period;

        /** 动态税种列单元格 */
        private List<ProjectTaxCellDTO> dynamicTaxCells;

        /** 实缴税金（固定表头） */
        private BigDecimal paidTotal;

        /** 申请税金（固定表头） */
        private BigDecimal declaredTotal;

        /** 差异（固定表头） */
        private BigDecimal differenceAmount;

        /** 原因（固定表头，手填） */
        private String reason;
    }

    @Data
    public static class ProjectTaxCellDTO {
        /** 表头展示名称（动态税种名） */
        private String headerName;

        /** 单元格金额值 */
        private BigDecimal value;
    }
}
