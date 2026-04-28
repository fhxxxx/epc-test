package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.SourceCopyLedgerSheetData;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 纯复制页 Renderer 抽象基类。
 */
public abstract class AbstractSourceCopySheetRenderer implements LedgerSheetRenderer<SourceCopyLedgerSheetData> {
    private final BlobStorageRemote blobStorageRemote;

    protected AbstractSourceCopySheetRenderer(BlobStorageRemote blobStorageRemote) {
        this.blobStorageRemote = blobStorageRemote;
    }

    @Override
    public void render(Workbook workbook, SourceCopyLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        if (data == null || isBlank(data.getSourceBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, support().getDisplayName() + "渲染数据为空");
        }

        TempWorkbookHandle sourceHandle = null;
        try {
            sourceHandle = loadWorkbookByTempFile(data.getSourceBlobPath(), "读取" + support().getDisplayName() + "上传文件失败");
            String sourceSheetName = resolveSourceSheetName(sourceHandle.workbook, data);
            SheetRenderSupport.copySourceSheet(
                    workbook,
                    support().getSheetName(),
                    support().getDisplayName(),
                    sourceHandle.workbook,
                    sourceSheetName
            );
        } finally {
            closeQuietly(sourceHandle);
        }
    }

    private String resolveSourceSheetName(Workbook sourceWorkbook, SourceCopyLedgerSheetData data) {
        if (!isBlank(data.getSourceSheetName()) && sourceWorkbook.getWorksheets().get(data.getSourceSheetName()) != null) {
            return data.getSourceSheetName();
        }
        if (!data.isFallbackToFirstSheet()) {
            throw new BizException(ErrorCode.BAD_REQUEST, support().getDisplayName() + "源文件不存在sheet: " + data.getSourceSheetName());
        }
        if (sourceWorkbook.getWorksheets().getCount() <= 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, support().getDisplayName() + "源文件不包含任何sheet");
        }
        Worksheet first = sourceWorkbook.getWorksheets().get(0);
        if (first == null || isBlank(first.getName())) {
            throw new BizException(ErrorCode.BAD_REQUEST, support().getDisplayName() + "源文件首个sheet无效");
        }
        return first.getName();
    }

    private TempWorkbookHandle loadWorkbookByTempFile(String blobPath, String errMsg) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("tax-ledger-source-copy-", ".xlsx");
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private void closeQuietly(TempWorkbookHandle handle) {
        if (handle == null) {
            return;
        }
        try {
            handle.close();
        } catch (Exception ignored) {
            // ignore cleanup errors
        }
    }

    private void tryDeleteFile(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            path.toFile().deleteOnExit();
        }
    }

    private class TempWorkbookHandle implements AutoCloseable {
        private final Workbook workbook;
        private final Path tempPath;

        private TempWorkbookHandle(Workbook workbook, Path tempPath) {
            this.workbook = workbook;
            this.tempPath = tempPath;
        }

        @Override
        public void close() {
            tryDeleteFile(tempPath);
        }
    }
}

