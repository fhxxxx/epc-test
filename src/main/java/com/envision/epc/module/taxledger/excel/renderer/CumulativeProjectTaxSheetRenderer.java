package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeProjectTaxLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 累计项目税收明细表 页渲染器。
 */
@Component
public class CumulativeProjectTaxSheetRenderer implements LedgerSheetRenderer<CumulativeProjectTaxLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }

    @Override
    public void render(Workbook workbook, CumulativeProjectTaxLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.CUMULATIVE_PROJECT_TAX.getSheetName(),
                LedgerSheetCode.CUMULATIVE_PROJECT_TAX.getDisplayName(),
                data.getPayload());
    }
}

