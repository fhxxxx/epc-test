package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

@Value
public class CumulativeProjectTaxLedgerSheetData implements LedgerSheetData {
    String sourceBlobPath;
    String sourceSheetName;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }

    @Override
    public Integer rowCount() {
        return 1;
    }
}

