package com.envision.epc.module.taxledger.excel.renderer;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import org.springframework.stereotype.Component;


/**
 * 增值税销项 页渲染器。
 */
@Component
public class VatOutputSheetRenderer extends AbstractSourceCopySheetRenderer {
    public VatOutputSheetRenderer(BlobStorageRemote blobStorageRemote) {
        super(blobStorageRemote);
    }

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_OUTPUT;
    }
}

