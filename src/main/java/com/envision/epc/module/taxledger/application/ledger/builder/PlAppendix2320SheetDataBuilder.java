package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.PlAppendix2320LedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import org.springframework.stereotype.Component;


/**
 * PL附表（2320/2355公司） 页数据构建器。
 */
@Component
public class PlAppendix2320SheetDataBuilder implements LedgerSheetDataBuilder<PlAppendix2320LedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL_APPENDIX_2320;
    }

    @Override
    public PlAppendix2320LedgerSheetData build(LedgerBuildContext ctx) {
        return new PlAppendix2320LedgerSheetData(
                SheetDataReaders.requireObject(ctx, FileCategoryEnum.PL_APPENDIX_2320, PlAppendix23202355DTO.class, LedgerSheetCode.PL_APPENDIX_2320));
    }
}

