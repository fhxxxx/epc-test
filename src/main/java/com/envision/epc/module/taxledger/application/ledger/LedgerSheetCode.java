package com.envision.epc.module.taxledger.application.ledger;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 台账 Sheet 定义
 */
@Getter
@RequiredArgsConstructor
public enum LedgerSheetCode {
    SUMMARY("Summary", "Summary", SheetGenerationMode.COMPUTED),
    BATCH1_DIRECT("Batch1_Direct", "Batch1_Direct", SheetGenerationMode.COMPUTED),
    BATCH2_CUMULATIVE("Batch2_Cumulative", "Batch2_Cumulative", SheetGenerationMode.COMPUTED),
    BATCH3_SUMMARY_REF("Batch3_SummaryRef", "Batch3_SummaryRef", SheetGenerationMode.COMPUTED);

    private final String sheetName;
    private final String displayName;
    private final SheetGenerationMode mode;
}
