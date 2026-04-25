package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.SummaryLedgerSheetData;
import com.envision.epc.module.taxledger.application.service.SummarySheetDataAssembler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummaryLedgerSheetDataBuilder implements LedgerSheetDataBuilder<SummaryLedgerSheetData> {
    private final SummarySheetDataAssembler summarySheetDataAssembler;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.SUMMARY;
    }

    @Override
    public SummaryLedgerSheetData build(LedgerBuildContext ctx) {
        SummarySheetDTO summary = summarySheetDataAssembler.assemble(
                ctx.getCompanyCode(),
                ctx.getYearMonth(),
                ctx.getFiles(),
                ctx.getNodeOutputs());
        return new SummaryLedgerSheetData(summary);
    }
}
