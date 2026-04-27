package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.PlLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 利润表（PL） 页渲染器。
 */
@Component
public class PlSheetRenderer implements LedgerSheetRenderer<PlLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL;
    }

    @Override
    public void render(Workbook workbook, PlLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.PL.getSheetName(),
                LedgerSheetCode.PL.getDisplayName(),
                data.getPayload());
    }
}

