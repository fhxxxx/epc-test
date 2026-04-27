package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.PlAppendixProjectLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import org.springframework.stereotype.Component;


/**
 * PL附表（项目公司） 页数据构建器。
 */
@Component
public class PlAppendixProjectSheetDataBuilder implements LedgerSheetDataBuilder<PlAppendixProjectLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL_APPENDIX_PROJECT;
    }

    @Override
    public PlAppendixProjectLedgerSheetData build(LedgerBuildContext ctx) {
        return new PlAppendixProjectLedgerSheetData(
                SheetDataReaders.requireList(ctx, FileCategoryEnum.PL_APPENDIX_PROJECT, PlAppendixProjectCompanyUploadDTO.class, LedgerSheetCode.PL_APPENDIX_PROJECT));
    }
}

