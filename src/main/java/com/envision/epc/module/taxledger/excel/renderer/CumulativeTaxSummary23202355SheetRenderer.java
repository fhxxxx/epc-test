package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeTaxSummary23202355LedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 累计税金汇总表-2320、2355 页渲染器。
 */
@Component
public class CumulativeTaxSummary23202355SheetRenderer implements LedgerSheetRenderer<CumulativeTaxSummary23202355LedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355;
    }

    @Override
    public void render(Workbook workbook, CumulativeTaxSummary23202355LedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355.getSheetName(),
                LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355.getDisplayName(),
                data.getPayload());
    }
}

