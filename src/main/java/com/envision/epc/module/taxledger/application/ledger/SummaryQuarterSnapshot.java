package com.envision.epc.module.taxledger.application.ledger;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Summary 季度预取快照（供 builder 消费，避免构建/渲染阶段查询数据源）。
 */
@Value
@Builder
public class SummaryQuarterSnapshot {
    /** 当前台账月份所属季度的三个月（yyyy-MM，固定3个）。 */
    List<String> quarterMonths;
    /** 该季度三个月对应的 PL 主营业务收入（本期发生额）。 */
    Map<String, BigDecimal> plMainRevenueByMonth;
    /** 缺失 PL 文件或缺失主营业务收入的月份。 */
    List<String> missingMonths;
    /** 预取阶段告警信息。 */
    List<String> warnings;
}

