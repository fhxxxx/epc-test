package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.TaxAccountingDifferenceMonitorLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 账税差异监控-2320、2355 页渲染器。
 */
@Component
public class TaxAccountingDifferenceMonitorSheetRenderer implements LedgerSheetRenderer<TaxAccountingDifferenceMonitorLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR;
    }

    @Override
    public void render(Workbook workbook, TaxAccountingDifferenceMonitorLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR.getSheetName(),
                LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR.getDisplayName(),
                data.getPayload());
    }
}

