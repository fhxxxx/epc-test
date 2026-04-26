package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatInputCertificationItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.Batch1DirectSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Batch1DirectSheetDataBuilder implements LedgerSheetDataBuilder<Batch1DirectSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BATCH1_DIRECT;
    }

    @Override
    public Batch1DirectSheetData build(LedgerBuildContext ctx) {
        BsAppendixUploadDTO bsAppendix = readOptionalObject(ctx, FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE, BsAppendixUploadDTO.class);
        VatOutputSheetUploadDTO vatOutput = readOptionalObject(ctx, FileCategoryEnum.VAT_OUTPUT, VatOutputSheetUploadDTO.class);
        List<VatInputCertificationItemDTO> vatInputRows =
                readOptionalList(ctx, FileCategoryEnum.VAT_INPUT_CERT, VatInputCertificationItemDTO.class);
        VatChangeAppendixUploadDTO vatChangeAppendix =
                readOptionalObject(ctx, FileCategoryEnum.VAT_CHANGE_APPENDIX, VatChangeAppendixUploadDTO.class);

        return Batch1DirectSheetData.builder()
                .message("Stage 1 direct sheets generated")
                .bsAppendix(bsAppendix)
                .vatOutput(vatOutput)
                .vatInputCertificationRows(vatInputRows)
                .vatChangeAppendix(vatChangeAppendix)
                .build();
    }

    private <T> T readOptionalObject(LedgerBuildContext ctx, FileCategoryEnum category, Class<T> clazz) {
        if (!hasCategory(ctx, category)) {
            return null;
        }
        return ctx.getParsedDataGateway().readParsedObject(category, clazz);
    }

    private <T> List<T> readOptionalList(LedgerBuildContext ctx, FileCategoryEnum category, Class<T> elementClazz) {
        if (!hasCategory(ctx, category)) {
            return List.of();
        }
        return ctx.getParsedDataGateway().readParsedList(category, elementClazz);
    }

    private boolean hasCategory(LedgerBuildContext ctx, FileCategoryEnum category) {
        if (ctx.getFiles() == null || category == null) {
            return false;
        }
        return ctx.getFiles().stream().anyMatch(file -> file != null && file.getFileCategory() == category);
    }
}
