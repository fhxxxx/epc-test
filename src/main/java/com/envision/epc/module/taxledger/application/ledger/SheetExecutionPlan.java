package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Sheet 执行顺序与启停规则
 */
@Component
public class SheetExecutionPlan {
    /**
     * Builder 执行顺序（依赖优先）
     */
    private static final List<LedgerSheetCode> BUILD_PLAN = List.of(
            LedgerSheetCode.BS,
            LedgerSheetCode.PL,
            LedgerSheetCode.BS_APPENDIX,
            LedgerSheetCode.PL_APPENDIX_2320,
            LedgerSheetCode.PL_APPENDIX_PROJECT,
            LedgerSheetCode.STAMP_TAX,
            LedgerSheetCode.STAMP_TAX_PROJECT,
            LedgerSheetCode.VAT_OUTPUT,
            LedgerSheetCode.VAT_INPUT_CERT,
            LedgerSheetCode.CUMULATIVE_PROJECT_TAX,
            LedgerSheetCode.VAT_CHANGE_APPENDIX,
            LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER,
            LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION,
            LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT,
            LedgerSheetCode.VAT_CHANGE,
//            LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT,
//            LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR,
            LedgerSheetCode.UNINVOICED_MONITOR,
            LedgerSheetCode.DL_INCOME,
            LedgerSheetCode.DL_OUTPUT,
            LedgerSheetCode.DL_INPUT,
            LedgerSheetCode.DL_OTHER,
            LedgerSheetCode.SUMMARY
//            LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355
    );

    /**
     * Renderer 实际执行顺序（依赖顺序）
     */
    private static final List<LedgerSheetCode> RENDER_EXECUTION_PLAN = List.of(
//            LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355,
            LedgerSheetCode.CUMULATIVE_PROJECT_TAX,
//            LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT,
//            LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR,
            LedgerSheetCode.BS,
            LedgerSheetCode.PL,
            LedgerSheetCode.DL_INCOME,
            LedgerSheetCode.DL_OUTPUT,
            LedgerSheetCode.DL_INPUT,
            LedgerSheetCode.VAT_OUTPUT,
            LedgerSheetCode.VAT_INPUT_CERT,
            LedgerSheetCode.VAT_CHANGE,
            LedgerSheetCode.SUMMARY,
            LedgerSheetCode.UNINVOICED_MONITOR,
            LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION,
            LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT,
            LedgerSheetCode.DL_OTHER,
            LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER,
            LedgerSheetCode.STAMP_TAX,
            LedgerSheetCode.STAMP_TAX_PROJECT
    );

    /**
     * Renderer 最终展示顺序（页签顺序）
     */
    private static final List<LedgerSheetCode> RENDER_DISPLAY_PLAN = List.of(
//            LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355,
            LedgerSheetCode.CUMULATIVE_PROJECT_TAX,
//            LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT,
//            LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR,
            LedgerSheetCode.BS,
            LedgerSheetCode.PL,
            LedgerSheetCode.DL_INCOME,
            LedgerSheetCode.DL_OUTPUT,
            LedgerSheetCode.DL_INPUT,
            LedgerSheetCode.SUMMARY,
            LedgerSheetCode.VAT_OUTPUT,
            LedgerSheetCode.VAT_INPUT_CERT,
            LedgerSheetCode.VAT_CHANGE,
            LedgerSheetCode.UNINVOICED_MONITOR,
            LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION,
            LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT,
            LedgerSheetCode.DL_OTHER,
            LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER,
            LedgerSheetCode.STAMP_TAX,
            LedgerSheetCode.STAMP_TAX_PROJECT
    );

    public SheetExecutionPlan() {
        validatePlanDefinition();
    }

    public List<LedgerSheetCode> orderedFor(String companyCode) {
        return orderedForRender(companyCode);
    }

    public List<LedgerSheetCode> orderedForBuild(String companyCode) {
        return BUILD_PLAN.stream()
                .filter(code -> isEnabledForCompany(code, companyCode))
                .toList();
    }

    public List<LedgerSheetCode> orderedForRender(String companyCode) {
        return orderedForDisplay(companyCode);
    }

    public List<LedgerSheetCode> orderedForRenderExecution(String companyCode) {
        return RENDER_EXECUTION_PLAN.stream()
                .filter(code -> isEnabledForCompany(code, companyCode))
                .filter(this::isRenderedInFinalLedger)
                .toList();
    }

    public List<LedgerSheetCode> orderedForDisplay(String companyCode) {
        return RENDER_DISPLAY_PLAN.stream()
                .filter(code -> isEnabledForCompany(code, companyCode))
                .filter(this::isRenderedInFinalLedger)
                .toList();
    }

    public List<LedgerSheetCode> allDefined() {
        Set<LedgerSheetCode> merged = new LinkedHashSet<>();
        merged.addAll(BUILD_PLAN);
        merged.addAll(RENDER_EXECUTION_PLAN);
        merged.addAll(RENDER_DISPLAY_PLAN);
        return new ArrayList<>(merged);
    }

    private boolean isRenderedInFinalLedger(LedgerSheetCode code) {
        if (code == LedgerSheetCode.PL_APPENDIX_2320
                || code == LedgerSheetCode.PL_APPENDIX_PROJECT
                || code == LedgerSheetCode.BS_APPENDIX
                || code == LedgerSheetCode.VAT_CHANGE_APPENDIX) {
            return false;
        }
        FileCategoryEnum category = code.getFileCategory();
        return category != null
                && category.isVisibleInFinalLedger()
                && category.getTargetSheetName() != null;
    }

    private boolean isEnabledForCompany(LedgerSheetCode code, String companyCode) {
        FileCategoryEnum category = code.getFileCategory();
        if (category == null) {
            return true;
        }
        return category.isAllowedForCompany(companyCode);
    }

    private void validatePlanDefinition() {
        assertNoDuplicate("BUILD_PLAN", BUILD_PLAN);
        assertNoDuplicate("RENDER_EXECUTION_PLAN", RENDER_EXECUTION_PLAN);
        assertNoDuplicate("RENDER_DISPLAY_PLAN", RENDER_DISPLAY_PLAN);

        Set<LedgerSheetCode> buildSet = new LinkedHashSet<>(BUILD_PLAN);
        for (LedgerSheetCode renderCode : RENDER_EXECUTION_PLAN) {
            if (!buildSet.contains(renderCode)) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Sheet执行计划定义错误: RENDER_EXECUTION_PLAN中的sheet未包含在BUILD_PLAN中, sheet=" + renderCode.name());
            }
        }
        for (LedgerSheetCode renderCode : RENDER_DISPLAY_PLAN) {
            if (!buildSet.contains(renderCode)) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Sheet执行计划定义错误: RENDER_DISPLAY_PLAN中的sheet未包含在BUILD_PLAN中, sheet=" + renderCode.name());
            }
        }
    }

    private void assertNoDuplicate(String planName, List<LedgerSheetCode> plan) {
        Set<LedgerSheetCode> set = new LinkedHashSet<>();
        for (LedgerSheetCode code : plan) {
            if (!set.add(code)) {
                throw new BizException(ErrorCode.INTERNAL_SERVER_ERROR,
                        "Sheet执行计划定义错误: " + planName + "存在重复sheet, sheet=" + code.name());
            }
        }
    }
}
