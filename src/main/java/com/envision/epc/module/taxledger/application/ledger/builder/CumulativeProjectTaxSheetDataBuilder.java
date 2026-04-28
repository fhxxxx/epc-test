package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

/**
 * 累计项目税收明细表 页数据构建器。
 * 纯复制模式。
 */
@Component
public class CumulativeProjectTaxSheetDataBuilder extends AbstractSourceCopySheetDataBuilder {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }

    @Override
    protected FileCategoryEnum sourceCategory() {
        return FileCategoryEnum.CUMULATIVE_PROJECT_TAX;
    }
}
