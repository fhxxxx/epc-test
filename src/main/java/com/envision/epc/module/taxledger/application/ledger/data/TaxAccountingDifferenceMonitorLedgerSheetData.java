package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

import java.util.List;

/**
 * 账税差异监控-2320、2355 页数据。
 */
@Value
public class TaxAccountingDifferenceMonitorLedgerSheetData implements LedgerSheetData {
    String ledgerYear;
    List<String> categoryTitles;
    List<TaxAccountingDifferenceMonitor23202355ItemDTO> rows;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR;
    }

    @Override
    public Integer rowCount() {
        return rows == null ? 0 : rows.size();
    }
}

