package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 增值税进项认证清单最小解析结果。
 */
@Data
public class VatInputCertParsedDTO {
    /**
     * 税额合计（SUM(taxAmount)）。
     */
    private BigDecimal taxAmountSum;
}
