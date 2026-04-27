package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.BsLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.BsStatementRowDTO;
import org.springframework.stereotype.Component;


/**
 * 资产负债表（BS） 页数据构建器。
 */
@Component
public class BsSheetDataBuilder implements LedgerSheetDataBuilder<BsLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BS;
    }

    @Override
    public BsLedgerSheetData build(LedgerBuildContext ctx) {
        return new BsLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.BS, BsStatementRowDTO.class, LedgerSheetCode.BS));
    }
}

