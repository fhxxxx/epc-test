package com.envision.epc.module.taxledger.application.ledger.data;

import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import lombok.Value;

/**
 * Summary表 页数据
 */
@Value
public class SummaryLedgerSheetData implements LedgerSheetData {
    SummarySheetDTO summary;

    @Override
    public LedgerSheetCode sheetCode() {
        return LedgerSheetCode.SUMMARY;
    }

    @Override
    public Integer rowCount() {
        int stamp = summary == null || summary.getStampDutyRows() == null ? 0 : summary.getStampDutyRows().size();
        int vat = summary == null || summary.getVatTaxRows() == null ? 0 : summary.getVatTaxRows().size();
        int common = summary == null || summary.getCommonTaxRows() == null ? 0 : summary.getCommonTaxRows().size();
        int cit = summary == null || summary.getCorporateIncomeTaxRows() == null ? 0 : summary.getCorporateIncomeTaxRows().size();
        return stamp + vat + common + cit;
    }
}
