package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
@Value

/**
 * 账税差异监控-2320、2355 页数据。
 */
public class TaxAccountingDifferenceMonitorLedgerSheetData implements LedgerSheetData {
    List<TaxAccountingDifferenceMonitor23202355ItemDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

