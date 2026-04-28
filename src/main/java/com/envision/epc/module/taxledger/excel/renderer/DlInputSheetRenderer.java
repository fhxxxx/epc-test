package com.envision.epc.module.taxledger.excel.renderer;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import org.springframework.stereotype.Component;

/**
 * 进项明细 页渲染器（纯复制）。
 */
@Component
public class DlInputSheetRenderer extends AbstractSourceCopySheetRenderer {
    public DlInputSheetRenderer(BlobStorageRemote blobStorageRemote) {
        super(blobStorageRemote);
    }

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.DL_INPUT;
    }
}

