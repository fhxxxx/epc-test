package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class StampDutyDetailRowDTO {

    /** 序号 */
    private String serialNo;

    /** 税目 */
    private String taxItem;

    /** 计税依据 */
    private BigDecimal taxableBasis;

    /** 税率（小数，如0.0003） */
    private BigDecimal taxRate;

    /** 应纳税额 */
    private BigDecimal taxPayableAmount;

    /** 减免税额 */
    private BigDecimal taxReductionAmount;

    /** 已缴税额 */
    private BigDecimal taxPaidAmount;

    /** 应补（退）税额 */
    private BigDecimal taxPayableOrRefundableAmount;
}
