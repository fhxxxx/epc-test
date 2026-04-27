package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeAppendixLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import org.springframework.stereotype.Component;


/**
 * 增值税变动表附表 页数据构建器。
 */
@Component
public class VatChangeAppendixSheetDataBuilder implements LedgerSheetDataBuilder<VatChangeAppendixLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_CHANGE_APPENDIX;
    }

    @Override
    public VatChangeAppendixLedgerSheetData build(LedgerBuildContext ctx) {
        return new VatChangeAppendixLedgerSheetData(
                SheetDataReaders.requireObject(ctx, FileCategoryEnum.VAT_CHANGE_APPENDIX, VatChangeAppendixUploadDTO.class, LedgerSheetCode.VAT_CHANGE_APPENDIX));
    }
}

