package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
@Value

/**
 * BS附表 页数据。
 */
public class BsAppendixLedgerSheetData implements LedgerSheetData {
    List<BsAppendixUploadDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.BS_APPENDIX;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

