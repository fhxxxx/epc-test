package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativeDeclarationLedgerSheetData;
import org.springframework.stereotype.Component;


/**
 * 项目累计申报 页渲染器。
 */
@Component
public class ProjectCumulativeDeclarationSheetRenderer implements LedgerSheetRenderer<ProjectCumulativeDeclarationLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION;
    }

    @Override
    public void render(Workbook workbook, ProjectCumulativeDeclarationLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        SheetRenderSupport.renderPayload(
                workbook,
                LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION.getSheetName(),
                LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION.getDisplayName(),
                data.getPayload());
    }
}

