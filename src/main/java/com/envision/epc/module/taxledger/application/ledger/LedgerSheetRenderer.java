package com.envision.epc.module.taxledger.application.ledger;

import com.aspose.cells.Workbook;

/**
 * Sheet 渲染器
 */
public interface LedgerSheetRenderer<T extends LedgerSheetData> {
    LedgerSheetCode support();

    void render(Workbook workbook, T data, LedgerRenderContext ctx) throws Exception;
}
