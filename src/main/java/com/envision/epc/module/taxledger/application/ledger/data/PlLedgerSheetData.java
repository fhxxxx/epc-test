package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

import java.util.List;

/**
 * 利润表（PL） 页渲染输入。
 */
@Value
public class PlLedgerSheetData implements LedgerSheetData {
    RenderMode renderMode;
    String targetSheetName;
    String previousSheetName;
    String currentPlBlobPath;
    String previousLedgerBlobPath;
    PlAppendix23202355DTO appendix2320Data;
    List<PlAppendixProjectCompanyUploadDTO> appendixProjectData;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.PL;
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

