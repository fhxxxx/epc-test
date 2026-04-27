package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeAppendixLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 增值税变动表附表 页渲染器。
 */
@Component
public class VatChangeAppendixSheetRenderer implements LedgerSheetRenderer<VatChangeAppendixLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_CHANGE_APPENDIX;
    }

    @Override
    public void render(Workbook workbook, VatChangeAppendixLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.VAT_CHANGE_APPENDIX.getSheetName(),
                LedgerSheetCode.VAT_CHANGE_APPENDIX.getDisplayName(),
                data.getPayload());
    }
}

