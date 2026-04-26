package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatInputCertificationItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Stage1 直接取数页数据（示例：承载已解析上传 DTO）
 */
@Value
@Builder
public class Batch1DirectSheetData implements LedgerSheetData {
    String message;
    BsAppendixUploadDTO bsAppendix;
    VatOutputSheetUploadDTO vatOutput;
    List<VatInputCertificationItemDTO> vatInputCertificationRows;
    VatChangeAppendixUploadDTO vatChangeAppendix;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.BATCH1_DIRECT;
    }

    @Override
    public Integer rowCount() {
        int inputRows = vatInputCertificationRows == null ? 0 : vatInputCertificationRows.size();
        return inputRows;
    }
}
