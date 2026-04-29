package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Value
public class ProjectCumulativeDeclarationLedgerSheetData implements LedgerSheetData {
    String ledgerYear;
    List<String> taxHeaders;
    List<RowData> monthRows;
    RowData totalRow;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION;
    }

    @Override
    public Integer rowCount() {
        int monthCount = monthRows == null ? 0 : monthRows.size();
        return totalRow == null ? monthCount : monthCount + 1;
    }

    @Value
    public static class RowData {
        String periodKey;
        String periodLabel;
        Map<String, BigDecimal> valuesByTaxHeader;
        BigDecimal taxTotal;
        boolean totalRow;
    }
}

