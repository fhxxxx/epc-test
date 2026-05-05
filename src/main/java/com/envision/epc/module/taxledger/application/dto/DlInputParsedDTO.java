package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据湖-进项明细最小解析结果。
 */
@Data
public class DlInputParsedDTO {
    /**
     * 货币金额合计（documentAmount）。
     */
    private BigDecimal documentAmountSum;

    /**
     * 按科目的本币金额合计（localAmount）。
     */
    private Map<String, BigDecimal> localAmountSumByAccount = new HashMap<>();
}
