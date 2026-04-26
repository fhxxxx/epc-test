package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.Batch1DirectSheetData;
import org.springframework.stereotype.Component;

@Component
public class Batch1DirectSheetRenderer implements LedgerSheetRenderer<Batch1DirectSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH1_DIRECT;
    }

    @Override
    public void render(Workbook workbook, Batch1DirectSheetData data, LedgerRenderContext ctx) {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(data.getSheetCode().getSheetName());
        sheet.getCells().get("A1").putValue(data.getMessage());
        sheet.getCells().get("A2").putValue("BS附表对象: " + (data.getBsAppendix() != null));
        sheet.getCells().get("A3").putValue("增值税销项对象: " + (data.getVatOutput() != null));
        sheet.getCells().get("A4").putValue("增值税进项认证行数: " + (data.getVatInputCertificationRows() == null ? 0 : data.getVatInputCertificationRows().size()));
        sheet.getCells().get("A5").putValue("增值税变动附表对象: " + (data.getVatChangeAppendix() != null));
    }
}
