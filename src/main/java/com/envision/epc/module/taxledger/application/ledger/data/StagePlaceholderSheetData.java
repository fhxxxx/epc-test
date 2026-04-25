package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

/**
 * 阶段占位页数据
 */
@Value
public class StagePlaceholderSheetData implements LedgerSheetData {
    LedgerSheetCode sheetCode;
    String message;

    @Override
    public LedgerSheetCode sheetCode() {
        return sheetCode;
    }
}
