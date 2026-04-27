package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
@Value

/**
 * 未开票数监控 页数据。
 */
public class UninvoicedMonitorLedgerSheetData implements LedgerSheetData {
    List<UninvoicedMonitorItemDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.UNINVOICED_MONITOR;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

