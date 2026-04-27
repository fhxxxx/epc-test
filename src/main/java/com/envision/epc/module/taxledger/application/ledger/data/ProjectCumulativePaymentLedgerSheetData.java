package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativePaymentSheetDTO;
@Value

/**
 * 项目累计缴纳 页数据。
 */
public class ProjectCumulativePaymentLedgerSheetData implements LedgerSheetData {
    ProjectCumulativePaymentSheetDTO payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : 1;
    }
}

