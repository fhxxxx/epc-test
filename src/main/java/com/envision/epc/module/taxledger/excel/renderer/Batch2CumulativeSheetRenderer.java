package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.StagePlaceholderSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch2CumulativeSheetRenderer implements LedgerSheetRenderer<StagePlaceholderSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH2_CUMULATIVE;
    }

    @Override
    public void render(Workbook workbook, StagePlaceholderSheetData data, LedgerRenderContext ctx) {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(data.getSheetCode().getSheetName());
        sheet.getCells().get("A1").putValue(data.getMessage());
    }
}
