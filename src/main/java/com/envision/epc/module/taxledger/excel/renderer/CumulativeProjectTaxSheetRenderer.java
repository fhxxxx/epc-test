package com.envision.epc.module.taxledger.excel.renderer;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import org.springframework.stereotype.Component;

/**
 * 累计项目税收明细表 页渲染器。
 * 纯复制模式。
 */
@Component
public class CumulativeProjectTaxSheetRenderer extends AbstractSourceCopySheetRenderer {
    public CumulativeProjectTaxSheetRenderer(BlobStorageRemote blobStorageRemote) {
        super(blobStorageRemote);
    }

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }
}
