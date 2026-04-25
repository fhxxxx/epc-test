package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.StagePlaceholderSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch1DirectSheetDataBuilder implements LedgerSheetDataBuilder<StagePlaceholderSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH1_DIRECT;
    }

    @Override
    public StagePlaceholderSheetData build(LedgerBuildContext ctx) {
        return new StagePlaceholderSheetData(support(), "Stage 1 direct sheets generated");
    }
}
