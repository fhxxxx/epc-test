package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
@Value

/**
 * 增值税变动表附表 页数据。
 */
public class VatChangeAppendixLedgerSheetData implements LedgerSheetData {
    VatChangeAppendixUploadDTO payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.VAT_CHANGE_APPENDIX;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : 1;
    }
}

