package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;


/**
 * 增值税销项 页数据构建器。
 */
@Component
public class VatOutputSheetDataBuilder extends AbstractSourceCopySheetDataBuilder {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_OUTPUT;
    }

    @Override
    protected FileCategoryEnum sourceCategory() {
        return FileCategoryEnum.VAT_OUTPUT;
    }
}

