package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
@Value

/**
 * PL附表（2320/2355公司） 页数据。
 */
public class PlAppendix2320LedgerSheetData implements LedgerSheetData {
    PlAppendix23202355DTO payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.PL_APPENDIX_2320;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : 1;
    }
}

