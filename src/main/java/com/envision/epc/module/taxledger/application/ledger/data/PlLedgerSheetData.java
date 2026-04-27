package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
@Value

/**
 * 利润表（PL） 页数据。
 */
public class PlLedgerSheetData implements LedgerSheetData {
    List<PlStatementRowDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.PL;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

