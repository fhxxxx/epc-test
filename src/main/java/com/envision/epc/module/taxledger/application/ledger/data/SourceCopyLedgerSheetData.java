package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

/**
 * 纯复制页渲染输入（从源文件复制sheet到目标sheet）。
 */
@Value
public class SourceCopyLedgerSheetData implements LedgerSheetData {
    LedgerSheetCode sheetCode;
    String sourceBlobPath;
    String sourceSheetName;
    boolean fallbackToFirstSheet;

    @Override
    public LedgerSheetCode sheetCode() {
        return sheetCode;
    }

    @Override
    public Integer rowCount() {
        return null;
    }
}
