package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.SummaryLedgerSheetData;
import org.springframework.stereotype.Component;

@Component
public class SummaryLedgerSheetDataBuilder implements LedgerSheetDataBuilder<SummaryLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.SUMMARY;
    }

    @Override
    public SummaryLedgerSheetData build(LedgerBuildContext ctx) {
        return new SummaryLedgerSheetData(new SummarySheetDTO());
    }
}
