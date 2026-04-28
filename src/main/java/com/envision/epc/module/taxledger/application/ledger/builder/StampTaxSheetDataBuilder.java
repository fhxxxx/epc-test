package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;


/**
 * 印花税明细-2320、2355 页数据构建器。
 */
@Component
public class StampTaxSheetDataBuilder extends AbstractSourceCopySheetDataBuilder {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX;
    }

    @Override
    protected FileCategoryEnum sourceCategory() {
        return FileCategoryEnum.STAMP_TAX;
    }
}

