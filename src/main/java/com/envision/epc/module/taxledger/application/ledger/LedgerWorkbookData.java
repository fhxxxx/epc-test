package com.envision.epc.module.taxledger.application.ledger;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Map;

/**
 * 整本台账的数据容器
 */
@Value
@Builder
public class LedgerWorkbookData {
    String companyCode;
    String yearMonth;

    @Singular("sheetData")
    Map<LedgerSheetCode, LedgerSheetData> sheets;

    @Singular("buildMetric")
    Map<LedgerSheetCode, Long> buildCostMs;

    public LedgerSheetData required(LedgerSheetCode code) {
        LedgerSheetData data = sheets == null ? null : sheets.get(code);
        if (data == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "缺少Sheet数据: " + code.getDisplayName());
        }
        return data;
    }
}
