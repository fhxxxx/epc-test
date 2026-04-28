package com.envision.epc.module.taxledger.excel.renderer;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import org.springframework.stereotype.Component;


/**
 * 印花税明细-2320、2355 页渲染器。
 */
@Component
public class StampTaxSheetRenderer extends AbstractSourceCopySheetRenderer {
    public StampTaxSheetRenderer(BlobStorageRemote blobStorageRemote) {
        super(blobStorageRemote);
    }

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX;
    }
}

