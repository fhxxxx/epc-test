package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.VatOutputLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 增值税销项 页渲染器。
 */
@Component
public class VatOutputSheetRenderer implements LedgerSheetRenderer<VatOutputLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_OUTPUT;
    }

    @Override
    public void render(Workbook workbook, VatOutputLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.copySourceSheet(
                workbook,
                LedgerSheetCode.VAT_OUTPUT.getSheetName(),
                LedgerSheetCode.VAT_OUTPUT.getDisplayName(),
                data.getSourceWorkbook(),
                data.getSourceSheetName());
    }
}

