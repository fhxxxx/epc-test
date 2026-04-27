package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.ContractStampDutyLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 合同印花税明细台账 页渲染器。
 */
@Component
public class ContractStampDutyLedgerSheetRenderer implements LedgerSheetRenderer<ContractStampDutyLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER;
    }

    @Override
    public void render(Workbook workbook, ContractStampDutyLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER.getSheetName(),
                LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER.getDisplayName(),
                data.getPayload());
    }
}

