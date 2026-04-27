package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
@Value

/**
 * 合同印花税明细台账 页数据。
 */
public class ContractStampDutyLedgerSheetData implements LedgerSheetData {
    List<ContractStampDutyLedgerItemDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}
