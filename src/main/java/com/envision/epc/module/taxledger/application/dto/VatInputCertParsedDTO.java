package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 增值税进项认证清单最小解析结果。
 */
@Data
public class VatInputCertParsedDTO {
    /**
     * 金额合计（SUM(amount)）。
     */
    private BigDecimal amountSum;
}
