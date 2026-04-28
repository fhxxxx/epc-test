package com.envision.epc.module.taxledger.excel.renderer;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import org.springframework.stereotype.Component;

/**
 * 其他科目明细 页渲染器（纯复制）。
 */
@Component
public class DlOtherSheetRenderer extends AbstractSourceCopySheetRenderer {
    public DlOtherSheetRenderer(BlobStorageRemote blobStorageRemote) {
        super(blobStorageRemote);
    }

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.DL_OTHER;
    }
}

