package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import org.springframework.stereotype.Component;


/**
 * 增值税变动表 页数据构建器。
 */
@Component
public class VatChangeSheetDataBuilder implements LedgerSheetDataBuilder<VatChangeLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_CHANGE;
    }

    @Override
    public VatChangeLedgerSheetData build(LedgerBuildContext ctx) {
        return new VatChangeLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.VAT_CHANGE, VatChangeRowDTO.class, LedgerSheetCode.VAT_CHANGE));
    }
}

