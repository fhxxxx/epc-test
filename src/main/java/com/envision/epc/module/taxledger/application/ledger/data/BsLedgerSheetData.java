package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.BsStatementRowDTO;
@Value

/**
 * 资产负债表（BS） 页数据。
 */
public class BsLedgerSheetData implements LedgerSheetData {
    List<BsStatementRowDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.BS;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

