package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.StampTaxLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import org.springframework.stereotype.Component;


/**
 * 印花税明细-2320、2355 页数据构建器。
 */
@Component
public class StampTaxSheetDataBuilder implements LedgerSheetDataBuilder<StampTaxLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX;
    }

    @Override
    public StampTaxLedgerSheetData build(LedgerBuildContext ctx) {
        return new StampTaxLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.STAMP_TAX, StampDutySummaryRowDTO.class, LedgerSheetCode.STAMP_TAX));
    }
}

