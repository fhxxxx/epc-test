package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PlStatementRowDTO {

    /** 项目 */
    private String itemName;

    /** 行 */
    private String lineNo;

    /** 本期发生额 */
    private BigDecimal currentPeriodAmount;

    /** 累计发生额 */
    private BigDecimal accumulatedAmount;
}
