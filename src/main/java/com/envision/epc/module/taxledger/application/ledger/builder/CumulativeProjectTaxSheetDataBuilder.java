package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeProjectTaxLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;


/**
 * 累计项目税收明细表 页数据构建器。
 */
@Component
public class CumulativeProjectTaxSheetDataBuilder implements LedgerSheetDataBuilder<CumulativeProjectTaxLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }

    @Override
    public CumulativeProjectTaxLedgerSheetData build(LedgerBuildContext ctx) {
        return new CumulativeProjectTaxLedgerSheetData(
                SheetDataReaders.requireObject(ctx, FileCategoryEnum.CUMULATIVE_PROJECT_TAX, Object.class, LedgerSheetCode.CUMULATIVE_PROJECT_TAX));
    }
}

