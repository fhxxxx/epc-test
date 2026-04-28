package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 睿景景程月结数据表-报税最小解析结果（按税率聚合）。
 */
@Data
public class MonthlySettlementTaxParsedDTO {
    /**
     * 原始明细行（用于调试与追溯），每行对应一个分段中的一条业务记录。
     */
    private List<MonthlyTaxSectionDTO> sections = new ArrayList<>();

    /**
     * 按税率聚合的数据，key 如 6% / 9% / 13%。
     */
    private Map<String, RateAggregate> aggregateByRate = new LinkedHashMap<>();

    @Data
    public static class RateAggregate {
        private BigDecimal incomeSum = BigDecimal.ZERO;
        private BigDecimal outputTaxSum = BigDecimal.ZERO;
        private BigDecimal invoicedIncomeSum = BigDecimal.ZERO;
        private BigDecimal invoicedTaxAmountSum = BigDecimal.ZERO;
    }
}
