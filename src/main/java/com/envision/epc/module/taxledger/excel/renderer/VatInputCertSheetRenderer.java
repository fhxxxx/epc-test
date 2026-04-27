package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.VatInputCertLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 增值税进项认证清单 页渲染器。
 */
@Component
public class VatInputCertSheetRenderer implements LedgerSheetRenderer<VatInputCertLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_INPUT_CERT;
    }

    @Override
    public void render(Workbook workbook, VatInputCertLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.copySourceSheet(
                workbook,
                LedgerSheetCode.VAT_INPUT_CERT.getSheetName(),
                LedgerSheetCode.VAT_INPUT_CERT.getDisplayName(),
                data.getSourceWorkbook(),
                data.getSourceSheetName());
    }
}

