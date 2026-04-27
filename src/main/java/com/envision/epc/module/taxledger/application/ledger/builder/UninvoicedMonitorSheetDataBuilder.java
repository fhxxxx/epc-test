package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.UninvoicedMonitorLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import org.springframework.stereotype.Component;


/**
 * 未开票数监控 页数据构建器。
 */
@Component
public class UninvoicedMonitorSheetDataBuilder implements LedgerSheetDataBuilder<UninvoicedMonitorLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.UNINVOICED_MONITOR;
    }

    @Override
    public UninvoicedMonitorLedgerSheetData build(LedgerBuildContext ctx) {
        return new UninvoicedMonitorLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.UNINVOICED_MONITOR, UninvoicedMonitorItemDTO.class, LedgerSheetCode.UNINVOICED_MONITOR));
    }
}

