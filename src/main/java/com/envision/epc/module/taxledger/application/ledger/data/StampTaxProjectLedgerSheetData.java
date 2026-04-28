package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import com.envision.epc.module.taxledger.application.dto.StampDutyDetailRowDTO;
import lombok.Value;

import java.util.List;

@Value

/**
 * 印花税明细--非2320、2355 页数据。
 */
public class StampTaxProjectLedgerSheetData implements LedgerSheetData {
    String companyName;
    List<StampDutyDetailRowDTO> payload;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.STAMP_TAX_PROJECT;
    }

    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

