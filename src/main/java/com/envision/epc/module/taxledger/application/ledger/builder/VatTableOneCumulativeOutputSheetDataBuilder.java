package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.VatTableOneCumulativeOutputLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
import org.springframework.stereotype.Component;


/**
 * 增值税表一 累计销项-2320、2355 页数据构建器。
 */
@Component
public class VatTableOneCumulativeOutputSheetDataBuilder implements LedgerSheetDataBuilder<VatTableOneCumulativeOutputLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT;
    }

    @Override
    public VatTableOneCumulativeOutputLedgerSheetData build(LedgerBuildContext ctx) {
        return new VatTableOneCumulativeOutputLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.VAT_TABLE_ONE_CUMULATIVE_OUTPUT, VatTableOneCumulativeOutputItemDTO.class, LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT));
    }
}

