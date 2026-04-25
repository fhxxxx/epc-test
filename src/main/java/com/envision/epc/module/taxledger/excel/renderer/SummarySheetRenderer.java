package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.SummaryLedgerSheetData;
import com.envision.epc.module.taxledger.excel.SummaryTemplateRenderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SummarySheetRenderer implements LedgerSheetRenderer<SummaryLedgerSheetData> {
    private final SummaryTemplateRenderService summaryTemplateRenderService;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.SUMMARY;
    }

    @Override
    public void render(Workbook workbook, SummaryLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        summaryTemplateRenderService.renderInto(workbook, data.getSummary());
    }
}
