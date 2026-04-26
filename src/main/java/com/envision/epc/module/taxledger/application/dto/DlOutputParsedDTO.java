package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 数据湖-销项明细最小解析结果。
 */
@Data
public class DlOutputParsedDTO {
    /**
     * 货币金额合计（documentAmount）。
     */
    private BigDecimal documentAmountSum;
}
