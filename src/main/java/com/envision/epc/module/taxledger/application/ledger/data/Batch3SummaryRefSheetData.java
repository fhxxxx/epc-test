package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.dto.PrecheckSnapshotDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Builder;
import lombok.Value;

/**
 * Stage3 汇总引用页数据（当前承载预校验快照）
 */
@Value
@Builder
public class Batch3SummaryRefSheetData implements LedgerSheetData {
    String message;
    PrecheckSnapshotDTO snapshot;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.BATCH3_SUMMARY_REF;
    }
}
