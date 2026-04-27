package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
@Value

/**
 * 增值税表一 累计销项-2320、2355 页数据。
 */
public class VatTableOneCumulativeOutputLedgerSheetData implements LedgerSheetData {
    List<VatTableOneCumulativeOutputItemDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

