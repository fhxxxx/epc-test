package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Stage2 累计计算页数据（当前承载节点输出快照）
 */
@Value
@Builder
public class Batch2CumulativeSheetData implements LedgerSheetData {
    String message;
    Map<String, Object> n20Output;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.BATCH2_CUMULATIVE;
    }
}
