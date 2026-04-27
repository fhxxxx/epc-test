package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.StampTaxLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 印花税明细-2320、2355 页渲染器。
 */
@Component
public class StampTaxSheetRenderer implements LedgerSheetRenderer<StampTaxLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX;
    }

    @Override
    public void render(Workbook workbook, StampTaxLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.STAMP_TAX.getSheetName(),
                LedgerSheetCode.STAMP_TAX.getDisplayName(),
                data.getPayload());
    }
}

