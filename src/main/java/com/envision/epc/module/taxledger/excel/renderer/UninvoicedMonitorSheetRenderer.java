package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.UninvoicedMonitorLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 未开票数监控 页渲染器。
 */
@Component
public class UninvoicedMonitorSheetRenderer implements LedgerSheetRenderer<UninvoicedMonitorLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.UNINVOICED_MONITOR;
    }

    @Override
    public void render(Workbook workbook, UninvoicedMonitorLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.UNINVOICED_MONITOR.getSheetName(),
                LedgerSheetCode.UNINVOICED_MONITOR.getDisplayName(),
                data.getPayload());
    }
}

