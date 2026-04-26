package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.Batch2CumulativeSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch2CumulativeSheetRenderer implements LedgerSheetRenderer<Batch2CumulativeSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH2_CUMULATIVE;
    }

    @Override
    public void render(Workbook workbook, Batch2CumulativeSheetData data, LedgerRenderContext ctx) {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(data.getSheetCode().getSheetName());
        sheet.getCells().get("A1").putValue(data.getMessage());
        sheet.getCells().get("A2").putValue("N20输出键数量: " + (data.getN20Output() == null ? 0 : data.getN20Output().size()));
    }
}
