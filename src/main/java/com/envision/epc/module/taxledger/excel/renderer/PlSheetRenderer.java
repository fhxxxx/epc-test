package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Range;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.PlLedgerSheetData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


/**
 * 利润表（PL） 页渲染器。
 */
@Component
@RequiredArgsConstructor
public class PlSheetRenderer implements LedgerSheetRenderer<PlLedgerSheetData> {
    private final BlobStorageRemote blobStorageRemote;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL;
    }

    @Override
    public void render(Workbook workbook, PlLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        if (data == null || isBlank(data.getCurrentPlBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "PL渲染数据为空");
        }

        TempWorkbookHandle currentPlHandle = null;
        TempWorkbookHandle previousLedgerHandle = null;
        try {
            currentPlHandle = loadWorkbookByTempFile(data.getCurrentPlBlobPath(), "读取PL上传文件失败");
            Worksheet currentPlSheet = currentPlHandle.workbook.getWorksheets().get(0);

            Rect currentPlRect = detectEffectiveRect(currentPlSheet.getCells());
            if (currentPlRect == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "PL源文件无有效数据区域");
            }
            currentPlRect = expandRectToFirstColumn(currentPlRect);

            Worksheet targetSheet;
            int currentBlockStartRow;
            int currentBlockStartCol;

            if (data.getRenderMode() == PlLedgerSheetData.RenderMode.APPEND_ON_PREVIOUS
                    && !isBlank(data.getPreviousLedgerBlobPath())) {
                previousLedgerHandle = loadWorkbookByTempFile(data.getPreviousLedgerBlobPath(), "读取前序最终台账失败");
                Worksheet previousPlSheet = findPreviousPlSheet(previousLedgerHandle.workbook, data.getPreviousSheetName());
                int targetIndex = workbook.getWorksheets().add();
                targetSheet = workbook.getWorksheets().get(targetIndex);
                targetSheet.copy(previousPlSheet);
                targetSheet.setName(data.getTargetSheetName());

                Rect previousMainRect = detectEffectiveRect(targetSheet.getCells());
                int alignedCol = previousMainRect == null ? 1 : previousMainRect.startCol;
                int appendStartRow = targetSheet.getCells().getMaxDataRow() + 4;
                if (appendStartRow < 0) {
                    appendStartRow = 0;
                }
                copyRect(currentPlSheet.getCells(), currentPlRect, targetSheet.getCells(), appendStartRow, alignedCol);
                currentBlockStartRow = appendStartRow;
                currentBlockStartCol = alignedCol;
            } else {
                int targetIndex = workbook.getWorksheets().add();
                targetSheet = workbook.getWorksheets().get(targetIndex);
                targetSheet.setName(data.getTargetSheetName());
                copyRect(currentPlSheet.getCells(), currentPlRect, targetSheet.getCells(), 0, 1);
                targetSheet.getCells().setColumnWidth(0, 0d);
                currentBlockStartRow = 0;
                currentBlockStartCol = 1;
            }

            int mainRightCol = currentBlockStartCol + currentPlRect.colCount - 1;
            int appendixStartCol = resolveVisibleStartCol(targetSheet.getCells(), mainRightCol + 2);
            int appendixStartRow = currentBlockStartRow + 5;
            if (data.getAppendix2320Data() != null) {
                renderAppendix2320(targetSheet.getCells(), appendixStartRow, appendixStartCol, data.getAppendix2320Data());
            } else if (data.getAppendixProjectData() != null && !data.getAppendixProjectData().isEmpty()) {
                renderAppendixProject(targetSheet.getCells(), appendixStartRow, appendixStartCol, data.getAppendixProjectData());
            }
        } finally {
            closeQuietly(previousLedgerHandle);
            closeQuietly(currentPlHandle);
        }
    }

    private Worksheet findPreviousPlSheet(Workbook previousLedgerWb, String preferredName) {
        if (!isBlank(preferredName)) {
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
            if (!name.endsWith("月PL")) {
                continue;
            }
            try {
                int month = Integer.parseInt(name.substring(0, name.indexOf("月PL")));
                if (month > bestMonth) {
                    bestMonth = month;
                    bestSheet = sheet;
                }
            } catch (Exception ignored) {
                // ignore
            }
        }
        if (bestSheet != null) {
            return bestSheet;
        }
        for (int i = 0; i < previousLedgerWb.getWorksheets().getCount(); i++) {
            Worksheet sheet = previousLedgerWb.getWorksheets().get(i);
            if (sheet == null || sheet.getName() == null) {
                continue;
            }
            String name = sheet.getName();
            if (name.contains("PL") || name.contains("利润表")) {
                return sheet;
            }
        }
        return previousLedgerWb.getWorksheets().get(0);
    }

    private void renderAppendix2320(Cells cells, int startRow, int startCol, PlAppendix23202355DTO dto) {
        int row = startRow;
        cells.get(row, startCol).putValue("");
        cells.get(row, startCol + 1).putValue("未开票收入");
        cells.get(row, startCol + 2).putValue("销项");
        cells.get(row, startCol + 3).putValue("已开票收入");
        cells.get(row, startCol + 4).putValue("已开票销项");
        row++;
        if (dto.getInvoicingSplitList() != null) {
            for (PlAppendix23202355DTO.InvoicingSplitItem item : dto.getInvoicingSplitList()) {
                cells.get(row, startCol).putValue(nvl(item.getSplitBasis()));
                putNumberOrText(cells, row, startCol + 1, item.getUninvoicedIncome());
                putNumberOrText(cells, row, startCol + 2, item.getOutputTax());
                putNumberOrText(cells, row, startCol + 3, item.getInvoicedIncome());
                putNumberOrText(cells, row, startCol + 4, item.getInvoicedOutputTax());
                row++;
            }
        }
        row++;
        cells.get(row, startCol).putValue("");
        cells.get(row, startCol + 1).putValue("申报金额");
        cells.get(row, startCol + 2).putValue("申报税额");
        row++;
        if (dto.getDeclarationSplitList() != null) {
            for (PlAppendix23202355DTO.DeclarationSplitItem item : dto.getDeclarationSplitList()) {
                cells.get(row, startCol).putValue(nvl(item.getSplitBasis()));
                putNumberOrText(cells, row, startCol + 1, item.getDeclaredAmount());
                putNumberOrText(cells, row, startCol + 2, item.getDeclaredTaxAmount());
                row++;
            }
        }
    }

    private void renderAppendixProject(Cells cells, int startRow, int startCol, List<PlAppendixProjectCompanyUploadDTO> rows) {
        int row = startRow;
        cells.get(row, startCol).putValue("拆分依据");
        cells.get(row, startCol + 1).putValue("主营业务收入");
        cells.get(row, startCol + 2).putValue("销项");
        row++;
        for (PlAppendixProjectCompanyUploadDTO item : rows) {
            cells.get(row, startCol).putValue(nvl(item.getSplitBasis()));
            putNumberOrText(cells, row, startCol + 1, item.getMainBusinessRevenue());
            putNumberOrText(cells, row, startCol + 2, item.getOutputTax());
            row++;
        }
    }

    private void putNumberOrText(Cells cells, int row, int col, Object value) {
        if (value == null) {
            cells.get(row, col).putValue("");
            return;
        }
        cells.get(row, col).putValue(String.valueOf(value));
    }

    private String nvl(String value) {
        return value == null ? "" : value;
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

    private Rect expandRectToFirstColumn(Rect detected) {
        if (detected == null || detected.startCol <= 0) {
            return detected;
        }
        return new Rect(detected.startRow, 0, detected.rowCount, detected.colCount + detected.startCol);
    }

    private int resolveVisibleStartCol(Cells cells, int preferredCol) {
        int col = Math.max(0, preferredCol);
        int maxProbe = Math.max(cells.getMaxColumn() + 20, col + 20);
        while (col <= maxProbe && cells.getColumnWidth(col) <= 0d) {
            col++;
        }
        return col;
    }

    private boolean hasContent(Cells cells, int row, int col) {
        Cell cell = cells.get(row, col);
        if (cell == null) {
            return false;
        }
        Object raw = cell.getValue();
        if (raw instanceof String s) {
            return !s.trim().isEmpty();
        }
        if (raw != null) {
            return true;
        }
        String display = cell.getDisplayStringValue();
        if (display != null && !display.trim().isEmpty()) {
            return true;
        }
        String value = cell.getStringValue();
        if (value != null && !value.trim().isEmpty()) {
            return true;
        }
        String formula = cell.getFormula();
        return formula != null && !formula.trim().isEmpty();
    }

    private void copyRect(Cells sourceCells, Rect sourceRect, Cells targetCells, int targetStartRow, int targetStartCol) throws Exception {
        Range sourceRange = sourceCells.createRange(sourceRect.startRow, sourceRect.startCol, sourceRect.rowCount, sourceRect.colCount);
        Range targetRange = targetCells.createRange(targetStartRow, targetStartCol, sourceRect.rowCount, sourceRect.colCount);
        targetRange.copy(sourceRange);
    }

    private TempWorkbookHandle loadWorkbookByTempFile(String blobPath, String errMsg) {
        Path tempPath = null;
        try {
            tempPath = Files.createTempFile("tax-ledger-pl-", ".xlsx");
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

