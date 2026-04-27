package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
@Value

/**
 * 累计税金汇总表-2320、2355 页数据。
 */
public class CumulativeTaxSummary23202355LedgerSheetData implements LedgerSheetData {
    List<CumulativeTaxSummary23202355ColumnDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

