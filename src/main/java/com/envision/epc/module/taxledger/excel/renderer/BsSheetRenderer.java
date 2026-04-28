package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Range;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.BsLedgerSheetData;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;


/**
 * 资产负债表（BS） 页渲染器。
 */
@Component
@RequiredArgsConstructor
public class BsSheetRenderer implements LedgerSheetRenderer<BsLedgerSheetData> {
    private final BlobStorageRemote blobStorageRemote;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.BS;
    }

    @Override
    public void render(Workbook workbook, BsLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        if (data == null
                || isBlank(data.getCurrentBsBlobPath())
                || isBlank(data.getCurrentBsAppendixBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "BS渲染数据为空");
        }

        TempWorkbookHandle currentBsHandle = null;
        TempWorkbookHandle currentBsAppendixHandle = null;
        TempWorkbookHandle previousLedgerHandle = null;
        try {
            currentBsHandle = loadWorkbookByTempFile(data.getCurrentBsBlobPath(), "读取BS上传文件失败");
            currentBsAppendixHandle = loadWorkbookByTempFile(data.getCurrentBsAppendixBlobPath(), "读取BS附表上传文件失败");
            Worksheet currentBsSheet = currentBsHandle.workbook.getWorksheets().get(0);
            Worksheet currentBsAppendixSheet = currentBsAppendixHandle.workbook.getWorksheets().get(0);

            Rect bsRect = detectEffectiveRect(currentBsSheet.getCells());
            if (bsRect == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "BS源文件无有效数据区域");
            }
            Rect appendixRect = detectEffectiveRect(currentBsAppendixSheet.getCells());
            if (appendixRect == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "BS附表源文件无有效数据区域");
            }

            Worksheet targetSheet;
            int currentBlockStartRow;
            int currentBlockStartCol;

            if (data.getRenderMode() == BsLedgerSheetData.RenderMode.APPEND_ON_PREVIOUS
                    && !isBlank(data.getPreviousLedgerBlobPath())) {
                previousLedgerHandle = loadWorkbookByTempFile(data.getPreviousLedgerBlobPath(), "读取前序最终台账失败");
                Worksheet previousBsSheet = findPreviousBsSheet(previousLedgerHandle.workbook, data.getPreviousSheetName());
                int targetIndex = workbook.getWorksheets().add();
                targetSheet = workbook.getWorksheets().get(targetIndex);
                targetSheet.copy(previousBsSheet);
                targetSheet.setName(data.getTargetSheetName());

                Rect previousMainRect = detectMainBsRect(targetSheet.getCells());
                int alignedCol = previousMainRect == null ? 1 : previousMainRect.startCol;
                int appendStartRow = targetSheet.getCells().getMaxDataRow() + 4;
                if (appendStartRow < 0) {
                    appendStartRow = 0;
                }
                copyRect(currentBsSheet.getCells(), bsRect, targetSheet.getCells(), appendStartRow, alignedCol);
                currentBlockStartRow = appendStartRow;
                currentBlockStartCol = alignedCol;
            } else {
                int targetIndex = workbook.getWorksheets().add();
                targetSheet = workbook.getWorksheets().get(targetIndex);
                targetSheet.setName(data.getTargetSheetName());
                copyRect(currentBsSheet.getCells(), bsRect, targetSheet.getCells(), 0, 1);
                targetSheet.getCells().setColumnWidth(0, 0d);
                currentBlockStartRow = 0;
                currentBlockStartCol = 1;
            }

            int mainRightCol = currentBlockStartCol + bsRect.colCount - 1;
            int existingRightCol = targetSheet.getCells().getMaxDataColumn();
            int appendixStartCol = Math.max(mainRightCol, existingRightCol) + 1;
            int appendixStartRow = currentBlockStartRow + 2;
            copyRect(currentBsAppendixSheet.getCells(), appendixRect, targetSheet.getCells(), appendixStartRow, appendixStartCol);
        } finally {
            closeQuietly(previousLedgerHandle);
            closeQuietly(currentBsAppendixHandle);
            closeQuietly(currentBsHandle);
        }
    }

    private Worksheet findPreviousBsSheet(Workbook previousLedgerWb, String preferredName) {
        if (preferredName != null && !preferredName.isBlank()) {
            Worksheet exact = previousLedgerWb.getWorksheets().get(preferredName);
            if (exact != null) {
                return exact;
            }
        }
        int bestMonth = -1;
        Worksheet bestSheet = null;
        for (int i = 0; i < previousLedgerWb.getWorksheets().getCount(); i++) {
            Worksheet sheet = previousLedgerWb.getWorksheets().get(i);
            if (sheet == null || sheet.getName() == null) {
                continue;
            }
            String name = sheet.getName().trim();
            if (!name.endsWith("月BS")) {
                continue;
            }
            try {
                int month = Integer.parseInt(name.substring(0, name.indexOf("月BS")));
                if (month > bestMonth) {
                    bestMonth = month;
                    bestSheet = sheet;
                }
            } catch (Exception ignored) {
                // ignore invalid name
            }
        }
        if (bestSheet != null) {
            return bestSheet;
        }
        for (int i = 0; i < previousLedgerWb.getWorksheets().getCount(); i++) {
            Worksheet sheet = previousLedgerWb.getWorksheets().get(i);
            if (sheet != null && sheet.getName() != null && sheet.getName().contains("资产负债")) {
                return sheet;
            }
        }
        return previousLedgerWb.getWorksheets().get(0);
    }

    private Rect detectMainBsRect(Cells cells) {
        Rect rect = detectEffectiveRect(cells);
        if (rect == null) {
            return null;
        }
        return rect;
    }

    private Rect detectEffectiveRect(Cells cells) {
        int maxRow = cells.getMaxDataRow();
        int maxCol = cells.getMaxDataColumn();
        if (maxRow < 0 || maxCol < 0) {
            return null;
        }
        int minRow = Integer.MAX_VALUE;
        int minCol = Integer.MAX_VALUE;
        int realMaxRow = -1;
        int realMaxCol = -1;
        for (int row = 0; row <= maxRow; row++) {
            for (int col = 0; col <= maxCol; col++) {
                if (!hasContent(cells, row, col)) {
                    continue;
                }
                minRow = Math.min(minRow, row);
                minCol = Math.min(minCol, col);
                realMaxRow = Math.max(realMaxRow, row);
                realMaxCol = Math.max(realMaxCol, col);
            }
        }
        if (realMaxRow < 0 || realMaxCol < 0) {
            return null;
        }
        return new Rect(minRow, minCol, realMaxRow - minRow + 1, realMaxCol - minCol + 1);
    }

    private boolean hasContent(Cells cells, int row, int col) {
        Cell cell = cells.get(row, col);
        if (cell == null || cell.getValue() == null) {
            return false;
        }
        String value = cell.getStringValue();
        return value != null && !value.trim().isEmpty();
    }

    private void copyRect(Cells sourceCells, Rect sourceRect, Cells targetCells, int targetStartRow, int targetStartCol) throws Exception {
        Range sourceRange = sourceCells.createRange(
                sourceRect.startRow,
                sourceRect.startCol,
                sourceRect.rowCount,
                sourceRect.colCount
        );
        Range targetRange = targetCells.createRange(
                targetStartRow,
                targetStartCol,
                sourceRect.rowCount,
                sourceRect.colCount
        );
        targetRange.copy(sourceRange);
    }

    private TempWorkbookHandle loadWorkbookByTempFile(String blobPath, String errMsg) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("tax-ledger-bs-", ".xlsx");
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

    private static class Rect {
        private final int startRow;
        private final int startCol;
        private final int rowCount;
        private final int colCount;

        private Rect(int startRow, int startCol, int rowCount, int colCount) {
            this.startRow = startRow;
            this.startCol = startCol;
            this.rowCount = rowCount;
            this.colCount = colCount;
        }
    }
}

