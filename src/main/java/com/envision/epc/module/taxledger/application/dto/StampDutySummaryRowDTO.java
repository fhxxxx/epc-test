package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StampDutySummaryRowDTO {

    /** 合同类别 */
    private String contractCategory;

    /** 应税金额 */
    private BigDecimal taxableAmount;

    /** 税率 */
    private BigDecimal taxRate;

    /** 应纳税额 */
    private BigDecimal taxPayableAmount;
}
