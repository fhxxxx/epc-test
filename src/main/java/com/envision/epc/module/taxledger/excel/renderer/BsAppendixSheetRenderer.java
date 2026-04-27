package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.BsAppendixLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * BS附表 页渲染器。
 */
@Component
public class BsAppendixSheetRenderer implements LedgerSheetRenderer<BsAppendixLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BS_APPENDIX;
    }

    @Override
    public void render(Workbook workbook, BsAppendixLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.BS_APPENDIX.getSheetName(),
                LedgerSheetCode.BS_APPENDIX.getDisplayName(),
                data.getPayload());
    }
}

