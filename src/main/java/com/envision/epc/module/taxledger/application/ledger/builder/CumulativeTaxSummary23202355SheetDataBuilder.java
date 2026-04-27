package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeTaxSummary23202355LedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
import org.springframework.stereotype.Component;


/**
 * 累计税金汇总表-2320、2355 页数据构建器。
 */
@Component
public class CumulativeTaxSummary23202355SheetDataBuilder implements LedgerSheetDataBuilder<CumulativeTaxSummary23202355LedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355;
    }

    @Override
    public CumulativeTaxSummary23202355LedgerSheetData build(LedgerBuildContext ctx) {
        return new CumulativeTaxSummary23202355LedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.CUMULATIVE_TAX_SUMMARY_2320_2355, CumulativeTaxSummary23202355ColumnDTO.class, LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355));
    }
}

