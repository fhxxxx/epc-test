package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
@Value

/**
 * 增值税变动表 页数据。
 */
public class VatChangeLedgerSheetData implements LedgerSheetData {
    String appendixBlobPath;
    String appendixSheetName;
    List<VatChangeRowDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.VAT_CHANGE;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

