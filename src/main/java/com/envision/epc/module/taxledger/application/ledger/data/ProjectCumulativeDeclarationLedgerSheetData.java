package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativeDeclarationSheetDTO;
@Value

/**
 * 项目累计申报 页数据。
 */
public class ProjectCumulativeDeclarationLedgerSheetData implements LedgerSheetData {
    ProjectCumulativeDeclarationSheetDTO payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : 1;
    }
}

