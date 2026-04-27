package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.VatTableOneCumulativeOutputLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 增值税表一 累计销项-2320、2355 页渲染器。
 */
@Component
public class VatTableOneCumulativeOutputSheetRenderer implements LedgerSheetRenderer<VatTableOneCumulativeOutputLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT;
    }

    @Override
    public void render(Workbook workbook, VatTableOneCumulativeOutputLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT.getSheetName(),
                LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT.getDisplayName(),
                data.getPayload());
    }
}

