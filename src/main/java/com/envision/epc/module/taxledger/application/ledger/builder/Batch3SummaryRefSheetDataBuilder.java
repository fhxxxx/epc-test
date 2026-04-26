package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.Batch3SummaryRefSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch3SummaryRefSheetDataBuilder implements LedgerSheetDataBuilder<Batch3SummaryRefSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH3_SUMMARY_REF;
    }

    @Override
    public Batch3SummaryRefSheetData build(LedgerBuildContext ctx) {
        return Batch3SummaryRefSheetData.builder()
                .message("Stage 3 summary/ref generated")
                .snapshot(ctx.getSnapshot())
                .build();
    }
}
