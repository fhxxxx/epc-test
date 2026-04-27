package com.envision.epc.module.taxledger.application.ledger.data;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
@Value

/**
 * 增值税销项 页数据。
 */
public class VatOutputLedgerSheetData implements LedgerSheetData {
    Workbook sourceWorkbook;
    String sourceSheetName;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.VAT_OUTPUT;
    }
    @Override
    public Integer rowCount() {
        if (sourceWorkbook == null || sourceSheetName == null || sourceSheetName.isBlank()) {
            return 0;
        }
        Worksheet source;
        try {
            source = sourceWorkbook.getWorksheets().get(sourceSheetName);
        } catch (Exception ignore) {
            return 0;
        }
        if (source == null) {
            return 0;
        }
        int maxDataRow = source.getCells().getMaxDataRow();
        return Math.max(maxDataRow + 1, 0);
    }
}

