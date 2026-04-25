package com.envision.epc.module.taxledger.application.ledger;

/**
 * Sheet 数据构建器
 */
public interface LedgerSheetDataBuilder<T extends LedgerSheetData> {
    LedgerSheetCode support();

    T build(LedgerBuildContext ctx);
}
