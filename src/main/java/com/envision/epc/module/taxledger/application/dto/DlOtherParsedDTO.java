package com.envision.epc.module.taxledger.application.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 数据湖-其他科目明细最小解析结果。
 */
@Data
public class DlOtherParsedDTO {
    /**
     * 按科目的货币金额合计（documentAmount）。
     */
    private Map<String, BigDecimal> documentAmountSumByAccount = new HashMap<>();

    /**
     * 按科目的本币金额合计（localAmount）。
     */
    private Map<String, BigDecimal> localAmountSumByAccount = new HashMap<>();
}
