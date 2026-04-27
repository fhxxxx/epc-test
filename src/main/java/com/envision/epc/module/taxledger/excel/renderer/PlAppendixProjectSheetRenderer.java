package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.PlAppendixProjectLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * PL附表（项目公司） 页渲染器。
 */
@Component
public class PlAppendixProjectSheetRenderer implements LedgerSheetRenderer<PlAppendixProjectLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL_APPENDIX_PROJECT;
    }

    @Override
    public void render(Workbook workbook, PlAppendixProjectLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.PL_APPENDIX_PROJECT.getSheetName(),
                LedgerSheetCode.PL_APPENDIX_PROJECT.getDisplayName(),
                data.getPayload());
    }
}

