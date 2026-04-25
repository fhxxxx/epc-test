package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.StagePlaceholderSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch2CumulativeSheetDataBuilder implements LedgerSheetDataBuilder<StagePlaceholderSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH2_CUMULATIVE;
    }

    @Override
    public StagePlaceholderSheetData build(LedgerBuildContext ctx) {
        return new StagePlaceholderSheetData(support(), "Stage 2 cumulative sheets generated");
    }
}
