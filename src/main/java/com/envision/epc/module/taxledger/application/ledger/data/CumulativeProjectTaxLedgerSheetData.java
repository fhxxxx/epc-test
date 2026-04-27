package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
@Value

/**
 * 累计项目税收明细表 页数据。
 */
public class CumulativeProjectTaxLedgerSheetData implements LedgerSheetData {
    Object payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : 1;
    }
}

