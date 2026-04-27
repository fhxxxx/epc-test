package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.ContractStampDutyLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import org.springframework.stereotype.Component;


/**
 * 合同印花税明细台账 页数据构建器。
 */
@Component
public class ContractStampDutyLedgerSheetDataBuilder implements LedgerSheetDataBuilder<ContractStampDutyLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER;
    }

    @Override
    public ContractStampDutyLedgerSheetData build(LedgerBuildContext ctx) {
        return new ContractStampDutyLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER, ContractStampDutyLedgerItemDTO.class, LedgerSheetCode.CONTRACT_STAMP_DUTY_LEDGER));
    }
}

