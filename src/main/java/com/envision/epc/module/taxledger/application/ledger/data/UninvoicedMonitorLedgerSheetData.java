package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

import java.util.List;

/**
 * 未开票数监控 页数据。
 */
@Value
public class UninvoicedMonitorLedgerSheetData implements LedgerSheetData {
    String ledgerYear;
    List<UninvoicedMonitorItemDTO> rows;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.UNINVOICED_MONITOR;
    }

    @Override
    public Integer rowCount() {
        return rows == null ? 0 : rows.size();
    }
}

