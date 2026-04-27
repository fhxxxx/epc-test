package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 增值税变动表 页渲染器。
 */
@Component
public class VatChangeSheetRenderer implements LedgerSheetRenderer<VatChangeLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_CHANGE;
    }

    @Override
    public void render(Workbook workbook, VatChangeLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.VAT_CHANGE.getSheetName(),
                LedgerSheetCode.VAT_CHANGE.getDisplayName(),
                data.getPayload());
    }
}

