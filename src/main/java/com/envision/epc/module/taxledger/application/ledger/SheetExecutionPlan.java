package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * Sheet 执行顺序与启停规则
 */
@Component
public class SheetExecutionPlan {
    private static final List<PlanItem> PLAN = buildPlan();

    public List<LedgerSheetCode> orderedFor(String companyCode) {
        return orderedForRender(companyCode);
    }

    public List<LedgerSheetCode> orderedForBuild(String companyCode) {
        return PLAN.stream()
                .filter(item -> item.enabledWhen.test(companyCode))
                .map(item -> item.code)
                .toList();
    }

    public List<LedgerSheetCode> orderedForRender(String companyCode) {
        return PLAN.stream()
                .filter(item -> item.enabledWhen.test(companyCode))
                .map(item -> item.code)
                .filter(this::isRenderedInFinalLedger)
                .toList();
    }

    public List<LedgerSheetCode> allDefined() {
        return PLAN.stream().map(item -> item.code).toList();
    }

    private static List<PlanItem> buildPlan() {
        return Arrays.stream(LedgerSheetCode.values())
                .filter(SheetExecutionPlan::isVisibleBusinessSheet)
                .sorted(Comparator
                        .comparingInt(SheetExecutionPlan::priority)
                        .thenComparingInt(code -> code.getFileCategory().ordinal()))
                .map(code -> new PlanItem(code, byScope(code)))
                .toList();
    }

    private static boolean isVisibleBusinessSheet(LedgerSheetCode code) {
        if (code == LedgerSheetCode.BS_APPENDIX) {
            return false;
        }
        FileCategoryEnum category = code.getFileCategory();
        return category != null
                && category.isVisibleInFinalLedger()
                && category.getTargetSheetName() != null;
    }

    private static int priority(LedgerSheetCode code) {
        if (code == LedgerSheetCode.BS || code == LedgerSheetCode.PL) {
            return 0;
        }
        if (code == LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR
                || code == LedgerSheetCode.UNINVOICED_MONITOR
                || code == LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355
                || code == LedgerSheetCode.VAT_CHANGE
                || code == LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT) {
            return 2;
        }
        if (code == LedgerSheetCode.SUMMARY) {
            return 3;
        }
        return 1;
    }

    private record PlanItem(LedgerSheetCode code, Predicate<String> enabledWhen) {
    }

    private boolean isRenderedInFinalLedger(LedgerSheetCode code) {
        return code != LedgerSheetCode.PL_APPENDIX_2320
                && code != LedgerSheetCode.PL_APPENDIX_PROJECT;
    }

    private static Predicate<String> byScope(LedgerSheetCode code) {
        FileCategoryEnum category = code.getFileCategory();
        if (category == null) {
            return companyCode -> true;
        }
        return category::isAllowedForCompany;
    }
}
