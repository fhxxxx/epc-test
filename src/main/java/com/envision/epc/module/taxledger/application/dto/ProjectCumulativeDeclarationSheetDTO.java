package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProjectCumulativeDeclarationSheetDTO {

    /** 全表二维结构：每个元素代表一行（一个所属期） */
    private List<List<ProjectTaxCellDTO>> rows;

    @Data
    public static class ProjectTaxCellDTO {
        /** 表头展示名称（来自当前sheet表头） */
        private String headerName;

        /** 所属期（如2025-01） */
        private String period;

        /** 单元格金额值 */
        private BigDecimal value;
    }
}
