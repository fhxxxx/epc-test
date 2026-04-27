package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.PlAppendix2320LedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * PL附表（2320/2355公司） 页渲染器。
 */
@Component
public class PlAppendix2320SheetRenderer implements LedgerSheetRenderer<PlAppendix2320LedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL_APPENDIX_2320;
    }

    @Override
    public void render(Workbook workbook, PlAppendix2320LedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.PL_APPENDIX_2320.getSheetName(),
                LedgerSheetCode.PL_APPENDIX_2320.getDisplayName(),
                data.getPayload());
    }
}

