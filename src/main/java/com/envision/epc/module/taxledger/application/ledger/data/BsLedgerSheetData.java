package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

/**
 * 资产负债表（BS） 页渲染输入。
 */
@Value
public class BsLedgerSheetData implements LedgerSheetData {
    RenderMode renderMode;
    String targetSheetName;
    String previousSheetName;
    String currentBsBlobPath;
    String currentBsAppendixBlobPath;
    String previousLedgerBlobPath;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.BS;
    }

    @Override
    public Integer rowCount() {
        return null;
    }

    public enum RenderMode {
        FIRST_BUILD,
        APPEND_ON_PREVIOUS
    }
}

