package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.StampTaxProjectLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import org.springframework.stereotype.Component;


/**
 * 印花税明细--非2320、2355 页数据构建器。
 */
@Component
public class StampTaxProjectSheetDataBuilder implements LedgerSheetDataBuilder<StampTaxProjectLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX_PROJECT;
    }

    @Override
    public StampTaxProjectLedgerSheetData build(LedgerBuildContext ctx) {
        return new StampTaxProjectLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.STAMP_TAX_PROJECT, StampDutySummaryRowDTO.class, LedgerSheetCode.STAMP_TAX_PROJECT));
    }
}

