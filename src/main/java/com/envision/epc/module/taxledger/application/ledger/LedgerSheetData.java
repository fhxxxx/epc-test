package com.envision.epc.module.taxledger.application.ledger;

/**
 * 单个 Sheet 数据载体
 */
public interface LedgerSheetData {
    LedgerSheetCode sheetCode();

    default Integer rowCount() {
        return null;
    }
}
