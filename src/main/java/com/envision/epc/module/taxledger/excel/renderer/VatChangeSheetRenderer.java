package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.BackgroundType;
import com.aspose.cells.BorderType;
import com.aspose.cells.Cell;
import com.aspose.cells.CellBorderType;
import com.aspose.cells.Cells;
import com.aspose.cells.Font;
import com.aspose.cells.Range;
import com.aspose.cells.Style;
import com.aspose.cells.TextAlignmentType;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 增值税变动表 页渲染器。
 */
@Component
@RequiredArgsConstructor
public class VatChangeSheetRenderer implements LedgerSheetRenderer<VatChangeLedgerSheetData> {
    private final BlobStorageRemote blobStorageRemote;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_CHANGE;
    }

    @Override
    public void render(Workbook workbook, VatChangeLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(LedgerSheetCode.VAT_CHANGE.getSheetName());
        Cells cells = sheet.getCells();

        int mainTableLastRow = renderMainTable(cells, data == null ? List.of() : data.getPayload());
        appendAppendixBelow(workbook, sheet, data, mainTableLastRow + 3, 0);
    }

    private int renderMainTable(Cells cells, List<VatChangeRowDTO> rows) {
        int headerRow = 0;
        int dataStartRow = 1;

        // 前两列(基础条目/拆分依据)不渲染，主表从A列顶格开始
        String[] headers = new String[]{
                "条目", "未开票金额", "当月开票金额", "以前月度开票金额", "合计"
        };
        for (int i = 0; i < headers.length; i++) {
            int col = i;
            Cell cell = cells.get(headerRow, col);
            cell.putValue(headers[i]);
            cell.setStyle(buildHeaderStyle(cells));
        }

        for (int i = 0; i < rows.size(); i++) {
            VatChangeRowDTO row = rows.get(i);
            int r = dataStartRow + i;
            writeTextCell(cells, r, 0, row == null ? "" : row.getItemName());
            writeUnbilledAmountCell(cells, r, row);
            writeAmountCell(cells, r, 2, row == null ? null : row.getCurrentMonthInvoicedAmount());
            writeAmountCell(cells, r, 3, row == null ? null : row.getPreviousMonthInvoicedAmount());
            writeAmountCell(cells, r, 4, row == null ? null : row.getTotalAmount());
        }
        applyColumnWidths(cells);
        return rows.isEmpty() ? headerRow : dataStartRow + rows.size() - 1;
    }

    private void appendAppendixBelow(Workbook targetWorkbook,
                                     Worksheet targetSheet,
                                     VatChangeLedgerSheetData data,
                                     int startRow,
                                     int startCol) throws Exception {
        if (data == null || isBlank(data.getAppendixBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "增值税变动表渲染失败: 缺少增值税变动表附表源文件");
        }

        TempWorkbookHandle appendixHandle = null;
        try {
            appendixHandle = loadWorkbookByTempFile(data.getAppendixBlobPath(), "读取增值税变动表附表上传文件失败");
            Worksheet appendixSheet;
            if (!isBlank(data.getAppendixSheetName())) {
                appendixSheet = appendixHandle.workbook.getWorksheets().get(data.getAppendixSheetName());
                if (appendixSheet == null) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "增值税变动表附表指定sheet不存在: " + data.getAppendixSheetName());
                }
            } else {
                appendixSheet = appendixHandle.workbook.getWorksheets().get(0);
            }

            Rect rect = detectEffectiveRect(appendixSheet.getCells());
            if (rect == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "增值税变动表附表源文件无有效数据区域");
            }
            copyRect(appendixSheet.getCells(), rect, targetSheet.getCells(), startRow, startCol);
        } finally {
            closeQuietly(appendixHandle);
        }
    }

    private void writeTextCell(Cells cells, int row, int col, String value) {
        Cell cell = cells.get(row, col);
        cell.putValue(isBlank(value) ? "-" : value);
        cell.setStyle(buildBodyTextStyle(cells));
    }

    private void writeAmountCell(Cells cells, int row, int col, BigDecimal value) {
        Cell cell = cells.get(row, col);
        Style style = buildBodyAmountStyle(cells);
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        cell.putValue(amount.doubleValue());
        cell.setStyle(style);
    }

    /**
     * 未开票金额列(B列)渲染规则：
     * 1) 当条目属于“当月利润表主营业务收入* / 应交增值税-销项税*”时，写入公式：合计-当月开票-以前月度开票。
     * 2) 其他条目保持原有写值逻辑。
     */
    private void writeUnbilledAmountCell(Cells cells, int row, VatChangeRowDTO dto) {
        if (!shouldUseUnbilledFormula(dto)) {
            writeAmountCell(cells, row, 1, dto == null ? null : dto.getUnbilledAmount());
            return;
        }
        Cell cell = cells.get(row, 1);
        cell.setFormula(buildUnbilledFormula(row));
        cell.setStyle(buildBodyAmountStyle(cells));
    }

    private String buildUnbilledFormula(int zeroBasedRow) {
        int excelRow = zeroBasedRow + 1;
        return "=E" + excelRow + "-C" + excelRow + "-D" + excelRow;
    }

    private boolean shouldUseUnbilledFormula(VatChangeRowDTO dto) {
        if (dto == null) {
            return false;
        }
        String base = normalizeBaseItem(dto.getBaseItem());
        if (isBlank(base)) {
            base = normalizeBaseItem(dto.getItemName());
        }
        return "当月利润表主营业务收入".equals(base) || "应交增值税-销项税".equals(base);
    }

    private String normalizeBaseItem(String text) {
        if (isBlank(text)) {
            return "";
        }
        return text.replace("*", "")
                .replace("＊", "")
                .replace(" ", "")
                .trim();
    }

    private void applyColumnWidths(Cells cells) {
        cells.setColumnWidth(0, 30);
        cells.setColumnWidth(1, 16);
        cells.setColumnWidth(2, 16);
        cells.setColumnWidth(3, 18);
        cells.setColumnWidth(4, 16);
    }

    private Style buildHeaderStyle(Cells cells) {
        Style style = cells.get(0, 0).getStyle();
        style.setPattern(BackgroundType.NONE);
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        Font font = style.getFont();
        font.setBold(true);
        applyBorder(style);
        return style;
    }

    private Style buildBodyTextStyle(Cells cells) {
        Style style = cells.get(1, 0).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.LEFT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        applyBorder(style);
        return style;
    }

    private Style buildBodyAmountStyle(Cells cells) {
        Style style = cells.get(1, 3).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.RIGHT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom("_(* #,##0.00_);_(* (#,##0.00);_(* \"-\"??_);_(@_)");
        applyBorder(style);
        return style;
    }

    private void applyBorder(Style style) {
        style.getBorders().getByBorderType(BorderType.TOP_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.BOTTOM_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.LEFT_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.RIGHT_BORDER).setLineStyle(CellBorderType.THIN);
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
                Cell cell = cells.get(row, col);
                if (cell == null || cell.getValue() == null) {
                    continue;
                }
                String value = cell.getStringValue();
                if (isBlank(value)) {
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
            tempPath = Files.createTempFile("tax-ledger-vat-change-appendix-", ".xlsx");
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

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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
