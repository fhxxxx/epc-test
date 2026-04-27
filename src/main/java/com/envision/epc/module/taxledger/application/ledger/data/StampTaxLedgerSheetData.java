package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
@Value

/**
 * 印花税明细-2320、2355 页数据。
 */
public class StampTaxLedgerSheetData implements LedgerSheetData {
    List<StampDutySummaryRowDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.STAMP_TAX;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

