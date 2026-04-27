package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.PlLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import org.springframework.stereotype.Component;


/**
 * 利润表（PL） 页数据构建器。
 */
@Component
public class PlSheetDataBuilder implements LedgerSheetDataBuilder<PlLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL;
    }

    @Override
    public PlLedgerSheetData build(LedgerBuildContext ctx) {
        return new PlLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.PL, PlStatementRowDTO.class, LedgerSheetCode.PL));
    }
}

