package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.VatOutputLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;


/**
 * 增值税销项 页数据构建器。
 */
@Component
public class VatOutputSheetDataBuilder implements LedgerSheetDataBuilder<VatOutputLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_OUTPUT;
    }

    @Override
    public VatOutputLedgerSheetData build(LedgerBuildContext ctx) {
        return new VatOutputLedgerSheetData(
                SheetDataReaders.requireSourceWorkbook(ctx, FileCategoryEnum.VAT_OUTPUT, LedgerSheetCode.VAT_OUTPUT),
                FileCategoryEnum.VAT_OUTPUT.getTargetSheetName());
    }
}

