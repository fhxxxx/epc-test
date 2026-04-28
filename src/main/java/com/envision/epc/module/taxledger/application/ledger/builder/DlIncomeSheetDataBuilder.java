package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

/**
 * 收入明细 页数据构建器（纯复制）。
 */
@Component
public class DlIncomeSheetDataBuilder extends AbstractSourceCopySheetDataBuilder {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.DL_INCOME;
    }

    @Override
    protected FileCategoryEnum sourceCategory() {
        return FileCategoryEnum.DL_INCOME;
    }
}

