package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeProjectTaxLedgerSheetData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 累计项目税收明细表 页渲染器。
 * 渲染阶段按 blobPath 加载源文件并复制 sheet，避免 builder 阶段占用大内存。
 */
@Component
@RequiredArgsConstructor
public class CumulativeProjectTaxSheetRenderer implements LedgerSheetRenderer<CumulativeProjectTaxLedgerSheetData> {
    private final BlobStorageRemote blobStorageRemote;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_PROJECT_TAX;
    }

    @Override
    public void render(Workbook workbook, CumulativeProjectTaxLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        if (data == null || isBlank(data.getSourceBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "累计项目税收明细表渲染数据为空");
        }

        TempWorkbookHandle sourceHandle = null;
        try {
            sourceHandle = loadWorkbookByTempFile(data.getSourceBlobPath(), "读取累计项目税收明细表源文件失败");
            Workbook sourceWorkbook = sourceHandle.workbook;

            String preferredName = isBlank(data.getSourceSheetName()) ? LedgerSheetCode.CUMULATIVE_PROJECT_TAX.getSheetName() : data.getSourceSheetName();
            Worksheet preferred = sourceWorkbook.getWorksheets().get(preferredName);
            String sourceSheetName = preferred == null
                    ? sourceWorkbook.getWorksheets().get(0).getName()
                    : preferred.getName();

            SheetRenderSupport.copySourceSheet(
                    workbook,
                    LedgerSheetCode.CUMULATIVE_PROJECT_TAX.getSheetName(),
                    LedgerSheetCode.CUMULATIVE_PROJECT_TAX.getDisplayName(),
                    sourceWorkbook,
                    sourceSheetName);
        } finally {
            closeQuietly(sourceHandle);
        }
    }

    private TempWorkbookHandle loadWorkbookByTempFile(String blobPath, String errMsg) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("tax-ledger-cpt-", ".xlsx");
            try (OutputStream output = Files.newOutputStream(tempPath)) {
                blobStorageRemote.loadStream(blobPath, output);
            }
            Workbook wb = new Workbook(tempPath.toString());
            return new TempWorkbookHandle(wb, tempPath);
        } catch (Exception ex) {
            if (tempPath != null) {
                tryDeleteFile(tempPath);
            }
            throw new BizException(ErrorCode.BAD_REQUEST, errMsg + ": " + ex.getMessage());
        }
    }

    private void closeQuietly(TempWorkbookHandle handle) {
        if (handle == null) {
            return;
        }
        try {
            if (handle.workbook != null) {
                handle.workbook.dispose();
            }
        } catch (Exception ignored) {
            // ignore
        }
        tryDeleteFile(handle.tempPath);
    }

    private void tryDeleteFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static class TempWorkbookHandle {
        private final Workbook workbook;
        private final Path tempPath;

        private TempWorkbookHandle(Workbook workbook, Path tempPath) {
            this.workbook = workbook;
            this.tempPath = tempPath;
        }
    }
}

