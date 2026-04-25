package com.envision.epc.module.taxledger.application.ledger;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Predicate;

/**
 * Sheet 执行顺序与启停规则
 */
@Component
public class SheetExecutionPlan {
    private static final List<PlanItem> PLAN = List.of(
            new PlanItem(LedgerSheetCode.SUMMARY, companyCode -> true),
            new PlanItem(LedgerSheetCode.BATCH1_DIRECT, companyCode -> true),
            new PlanItem(LedgerSheetCode.BATCH2_CUMULATIVE, companyCode -> true),
            new PlanItem(LedgerSheetCode.BATCH3_SUMMARY_REF, companyCode -> true)
    );

    public List<LedgerSheetCode> orderedFor(String companyCode) {
        return PLAN.stream()
                .filter(item -> item.enabledWhen.test(companyCode))
                .map(item -> item.code)
                .toList();
    }

    public List<LedgerSheetCode> allDefined() {
        return PLAN.stream().map(item -> item.code).toList();
    }

    private record PlanItem(LedgerSheetCode code, Predicate<String> enabledWhen) {
    }
}
