package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.Batch3SummaryRefSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch3SummaryRefSheetRenderer implements LedgerSheetRenderer<Batch3SummaryRefSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH3_SUMMARY_REF;
    }

    @Override
    public void render(Workbook workbook, Batch3SummaryRefSheetData data, LedgerRenderContext ctx) {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(data.getSheetCode().getSheetName());
        sheet.getCells().get("A1").putValue(data.getMessage());
        sheet.getCells().get("A2").putValue("Precheck快照存在: " + (data.getSnapshot() != null));
    }
}
