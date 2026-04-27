package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.TaxAccountingDifferenceMonitorLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
import org.springframework.stereotype.Component;


/**
 * 账税差异监控-2320、2355 页数据构建器。
 */
@Component
public class TaxAccountingDifferenceMonitorSheetDataBuilder implements LedgerSheetDataBuilder<TaxAccountingDifferenceMonitorLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR;
    }

    @Override
    public TaxAccountingDifferenceMonitorLedgerSheetData build(LedgerBuildContext ctx) {
        return new TaxAccountingDifferenceMonitorLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.TAX_ACCOUNTING_DIFFERENCE_MONITOR, TaxAccountingDifferenceMonitor23202355ItemDTO.class, LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR));
    }
}

