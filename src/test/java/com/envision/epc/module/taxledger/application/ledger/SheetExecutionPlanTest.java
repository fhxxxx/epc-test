package com.envision.epc.module.taxledger.application.ledger;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SheetExecutionPlanTest {

    @Test
    void shouldOnlyContainVisibleBusinessSheetsAndSummaryLast() {
        SheetExecutionPlan plan = new SheetExecutionPlan();
        List<LedgerSheetCode> codes = plan.allDefined();

        assertFalse(codes.isEmpty());
        assertEquals(LedgerSheetCode.SUMMARY, codes.get(codes.size() - 1));
        assertTrue(codes.stream().noneMatch(code -> code.name().startsWith("BATCH")));

        List<LedgerSheetCode> expected = Arrays.stream(LedgerSheetCode.values())
                .filter(code -> code.getFileCategory().isVisibleInFinalLedger())
                .filter(code -> code.getFileCategory().getTargetSheetName() != null)
                .toList();
        assertEquals(expected.size(), codes.size());
        assertTrue(codes.containsAll(expected));
    }
}
