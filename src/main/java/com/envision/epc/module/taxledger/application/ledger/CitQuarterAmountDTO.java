package com.envision.epc.module.taxledger.application.ledger;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CitQuarterAmountDTO {
    private BigDecimal q1 = BigDecimal.ZERO;
    private BigDecimal q2 = BigDecimal.ZERO;
    private BigDecimal q3 = BigDecimal.ZERO;
    private BigDecimal q4 = BigDecimal.ZERO;
}

