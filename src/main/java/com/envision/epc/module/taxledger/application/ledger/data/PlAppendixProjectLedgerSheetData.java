package com.envision.epc.module.taxledger.application.ledger.data;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;
import java.util.List;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
@Value

/**
 * PL附表（项目公司） 页数据。
 */
public class PlAppendixProjectLedgerSheetData implements LedgerSheetData {
    List<PlAppendixProjectCompanyUploadDTO> payload;
    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.PL_APPENDIX_PROJECT;
    }
    @Override
    public Integer rowCount() {
        return payload == null ? 0 : payload.size();
    }
}

