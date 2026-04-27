package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.StampTaxProjectLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 印花税明细--非2320、2355 页渲染器。
 */
@Component
public class StampTaxProjectSheetRenderer implements LedgerSheetRenderer<StampTaxProjectLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX_PROJECT;
    }

    @Override
    public void render(Workbook workbook, StampTaxProjectLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.STAMP_TAX_PROJECT.getSheetName(),
                LedgerSheetCode.STAMP_TAX_PROJECT.getDisplayName(),
                data.getPayload());
    }
}

