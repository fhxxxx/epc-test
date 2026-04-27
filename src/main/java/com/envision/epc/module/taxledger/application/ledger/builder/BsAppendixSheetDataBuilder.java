package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.BsAppendixLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import org.springframework.stereotype.Component;


/**
 * BS附表 页数据构建器。
 */
@Component
public class BsAppendixSheetDataBuilder implements LedgerSheetDataBuilder<BsAppendixLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BS_APPENDIX;
    }

    @Override
    public BsAppendixLedgerSheetData build(LedgerBuildContext ctx) {
        return new BsAppendixLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE, BsAppendixUploadDTO.class, LedgerSheetCode.BS_APPENDIX));
    }
}

