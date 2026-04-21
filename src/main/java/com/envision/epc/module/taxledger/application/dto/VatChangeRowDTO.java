package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class VatChangeRowDTO {

    /** 基础条目 */
    private String baseItem;

    /** 拆分依据 */
    private String splitBasis;

    /** 条目 */
    private String itemName;

    /** 未开票金额 */
    private BigDecimal unbilledAmount;

    /** 当月开票金额 */
    private BigDecimal currentMonthInvoicedAmount;

    /** 以前月度开票金额 */
    private BigDecimal previousMonthInvoicedAmount;

    /** 合计 */
    private BigDecimal totalAmount;
}
