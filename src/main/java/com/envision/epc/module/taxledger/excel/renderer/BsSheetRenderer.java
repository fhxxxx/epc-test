package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.BsLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 资产负债表（BS） 页渲染器。
 */
@Component
public class BsSheetRenderer implements LedgerSheetRenderer<BsLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BS;
    }

    @Override
    public void render(Workbook workbook, BsLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.BS.getSheetName(),
                LedgerSheetCode.BS.getDisplayName(),
                data.getPayload());
    }
}

