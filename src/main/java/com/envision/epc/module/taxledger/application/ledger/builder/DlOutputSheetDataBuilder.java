package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

/**
 * 销项明细 页数据构建器（纯复制）。
 */
@Component
public class DlOutputSheetDataBuilder extends AbstractSourceCopySheetDataBuilder {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.DL_OUTPUT;
    }

    @Override
    protected FileCategoryEnum sourceCategory() {
        return FileCategoryEnum.DL_OUTPUT;
    }
}

