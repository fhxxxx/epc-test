package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativePaymentLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 项目累计缴纳 页渲染器。
 */
@Component
public class ProjectCumulativePaymentSheetRenderer implements LedgerSheetRenderer<ProjectCumulativePaymentLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT;
    }

    @Override
    public void render(Workbook workbook, ProjectCumulativePaymentLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT.getSheetName(),
                LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT.getDisplayName(),
                data.getPayload());
    }
}

