package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.BusinessSheetData;

/**
 * 源文件 sheet 复制型渲染器。
 */
public abstract class AbstractSourceSheetCopyRenderer<T extends BusinessSheetData.SourceSheetData>
        implements LedgerSheetRenderer<T> {
    protected abstract LedgerSheetCode code();

    @Override
    public LedgerSheetCode support() {
        return code();
    }

    @Override
    public void render(Workbook workbook, T data, LedgerRenderContext ctx) throws Exception {
        if (data == null || data.getSourceWorkbook() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "源sheet数据为空: " + code().getDisplayName());
        }
        Worksheet source = data.getSourceWorkbook().getWorksheets().get(data.getSourceSheetName());
        if (source == null) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "源sheet不存在: " + code().getDisplayName() + " - " + data.getSourceSheetName());
        }

        int index = workbook.getWorksheets().add();
        Worksheet target = workbook.getWorksheets().get(index);
        target.copy(source);
        target.setName(resolveSheetName(workbook, code().getSheetName(), index));
    }

    private String resolveSheetName(Workbook workbook, String baseName, int currentIndex) {
        if (workbook.getWorksheets().get(baseName) == null
                || workbook.getWorksheets().get(baseName).getIndex() == currentIndex) {
            return baseName;
        }
        int suffix = 1;
        while (workbook.getWorksheets().get(baseName + "_" + suffix) != null) {
            suffix++;
        }
        return baseName + "_" + suffix;
    }
}
