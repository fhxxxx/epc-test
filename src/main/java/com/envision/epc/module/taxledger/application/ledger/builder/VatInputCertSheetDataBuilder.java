package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.VatInputCertLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;


/**
 * 增值税进项认证清单 页数据构建器。
 */
@Component
public class VatInputCertSheetDataBuilder implements LedgerSheetDataBuilder<VatInputCertLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_INPUT_CERT;
    }

    @Override
    public VatInputCertLedgerSheetData build(LedgerBuildContext ctx) {
        return new VatInputCertLedgerSheetData(
                SheetDataReaders.requireSourceWorkbook(ctx, FileCategoryEnum.VAT_INPUT_CERT, LedgerSheetCode.VAT_INPUT_CERT),
                FileCategoryEnum.VAT_INPUT_CERT.getTargetSheetName());
    }
}

