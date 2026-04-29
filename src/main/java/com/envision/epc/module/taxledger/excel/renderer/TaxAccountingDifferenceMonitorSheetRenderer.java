package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.BackgroundType;
import com.aspose.cells.BorderType;
import com.aspose.cells.Cell;
import com.aspose.cells.CellBorderType;
import com.aspose.cells.Cells;
import com.aspose.cells.Color;
import com.aspose.cells.Font;
import com.aspose.cells.Style;
import com.aspose.cells.TextAlignmentType;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.TaxAccountingDifferenceMonitorLedgerSheetData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


/**
 * 账税差异监控-2320、2355 页渲染器。
 */
@Component
public class TaxAccountingDifferenceMonitorSheetRenderer implements LedgerSheetRenderer<TaxAccountingDifferenceMonitorLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR;
    }

    @Override
    public void render(Workbook workbook, TaxAccountingDifferenceMonitorLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        int sheetIndex = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(sheetIndex);
        sheet.setName(LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR.getSheetName());
        Cells cells = sheet.getCells();

        // A列和第一行留空，表格从 B2 开始。
        cells.setColumnWidth(0, 2.5d);
        List<String> categoryTitles = data == null || data.getCategoryTitles() == null ? List.of() : data.getCategoryTitles();
        List<TaxAccountingDifferenceMonitor23202355ItemDTO> rows = data == null || data.getRows() == null ? List.of() : data.getRows();

        int startRow = 1;
        int startCol = 1;
        int summaryStartCol = startCol + 1 + categoryTitles.size() * 2;
        int diffCol = summaryStartCol + 2;
        int analysisCol = diffCol + 1;
        int tableEndCol = analysisCol;

        mergeHeader(cells, startRow, startCol, categoryTitles, summaryStartCol, diffCol, analysisCol);
        renderHeader(cells, startRow, startCol, categoryTitles, summaryStartCol, diffCol, analysisCol);
        renderRows(cells, startRow + 2, startCol, summaryStartCol, diffCol, analysisCol, rows);
        applyColumnWidths(cells, startCol, categoryTitles.size(), summaryStartCol, diffCol, analysisCol);
        applyTableBorder(cells, startRow, startRow + 1 + rows.size(), startCol, tableEndCol);
    }

    private void mergeHeader(Cells cells,
                             int startRow,
                             int startCol,
                             List<String> categoryTitles,
                             int summaryStartCol,
                             int diffCol,
                             int analysisCol) {
        cells.merge(startRow, startCol, 2, 1);
        for (int i = 0; i < categoryTitles.size(); i++) {
            int groupStartCol = startCol + 1 + i * 2;
            cells.merge(startRow, groupStartCol, 1, 2);
        }
        cells.merge(startRow, summaryStartCol, 1, 2);
        cells.merge(startRow, diffCol, 2, 1);
        cells.merge(startRow, analysisCol, 2, 1);
    }

    private void renderHeader(Cells cells,
                              int startRow,
                              int startCol,
                              List<String> categoryTitles,
                              int summaryStartCol,
                              int diffCol,
                              int analysisCol) {
        writeHeaderCell(cells, startRow, startCol, "日期");
        for (int i = 0; i < categoryTitles.size(); i++) {
            int groupStartCol = startCol + 1 + i * 2;
            writeHeaderCell(cells, startRow, groupStartCol, categoryTitles.get(i));
            writeHeaderCell(cells, startRow + 1, groupStartCol, "账面收入");
            writeHeaderCell(cells, startRow + 1, groupStartCol + 1, "申报收入");
        }
        writeHeaderCell(cells, startRow, summaryStartCol, "汇总");
        writeHeaderCell(cells, startRow + 1, summaryStartCol, "账面收入");
        writeHeaderCell(cells, startRow + 1, summaryStartCol + 1, "增值税申报收入");

        writeHeaderCell(cells, startRow, diffCol, "账税差异");
        writeHeaderCell(cells, startRow, analysisCol, "差异分析");
    }

    private void renderRows(Cells cells,
                            int dataStartRow,
                            int startCol,
                            int summaryStartCol,
                            int diffCol,
                            int analysisCol,
                            List<TaxAccountingDifferenceMonitor23202355ItemDTO> rows) {
        for (int i = 0; i < rows.size(); i++) {
            int rowIndex = dataStartRow + i;
            TaxAccountingDifferenceMonitor23202355ItemDTO row = rows.get(i);
            writeTextCell(cells, rowIndex, startCol, row == null ? "" : row.getPeriod(), true);

            List<TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem> categories =
                    row == null || row.getCategoryIncomeList() == null ? new ArrayList<>() : row.getCategoryIncomeList();
            int col = startCol + 1;
            for (TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem category : categories) {
                if (col + 1 >= summaryStartCol) {
                    break;
                }
                writeAmountCell(cells, rowIndex, col, category == null ? null : category.getBookIncome());
                writeAmountCell(cells, rowIndex, col + 1, category == null ? null : category.getDeclaredIncome());
                col += 2;
            }
            while (col < summaryStartCol) {
                writeAmountCell(cells, rowIndex, col, null);
                col++;
            }

            writeAmountCell(cells, rowIndex, summaryStartCol, row == null ? null : row.getTotalBookIncome());
            writeAmountCell(cells, rowIndex, summaryStartCol + 1, row == null ? null : row.getTotalVatDeclaredIncome());
            writeAmountCell(cells, rowIndex, diffCol, row == null ? null : row.getAccountingTaxDifference());
            writeTextCell(cells, rowIndex, analysisCol, row == null ? "" : row.getDifferenceAnalysis(), false);
        }
    }

    private void applyColumnWidths(Cells cells,
                                   int startCol,
                                   int dynamicGroupCount,
                                   int summaryStartCol,
                                   int diffCol,
                                   int analysisCol) {
        cells.setColumnWidth(startCol, 12);
        for (int i = 0; i < dynamicGroupCount * 2; i++) {
            cells.setColumnWidth(startCol + 1 + i, 14);
        }
        cells.setColumnWidth(summaryStartCol, 14);
        cells.setColumnWidth(summaryStartCol + 1, 18);
        cells.setColumnWidth(diffCol, 14);
        cells.setColumnWidth(analysisCol, 18);
    }

    private void applyTableBorder(Cells cells, int startRow, int endRow, int startCol, int endCol) {
        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                Cell cell = cells.get(row, col);
                Style style = cell.getStyle();
                style.getBorders().getByBorderType(BorderType.TOP_BORDER).setLineStyle(CellBorderType.THIN);
                style.getBorders().getByBorderType(BorderType.BOTTOM_BORDER).setLineStyle(CellBorderType.THIN);
                style.getBorders().getByBorderType(BorderType.LEFT_BORDER).setLineStyle(CellBorderType.THIN);
                style.getBorders().getByBorderType(BorderType.RIGHT_BORDER).setLineStyle(CellBorderType.THIN);
                cell.setStyle(style);
            }
        }
    }

    private void writeHeaderCell(Cells cells, int row, int col, String text) {
        Cell cell = cells.get(row, col);
        cell.putValue(text);
        Style style = cell.getStyle();
        style.setPattern(BackgroundType.SOLID);
        style.setForegroundColor(Color.fromArgb(255, 255, 0));
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        Font font = style.getFont();
        font.setBold(true);
        cell.setStyle(style);
    }

    private void writeAmountCell(Cells cells, int row, int col, BigDecimal amount) {
        Cell cell = cells.get(row, col);
        Style style = cell.getStyle();
        style.setHorizontalAlignment(TextAlignmentType.RIGHT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom("#,##0.00;-#,##0.00;-");
        if (amount == null) {
            cell.putValue("-");
            style.setCustom("@");
        } else {
            cell.putValue(amount.doubleValue());
        }
        cell.setStyle(style);
    }

    private void writeTextCell(Cells cells, int row, int col, String text, boolean center) {
        Cell cell = cells.get(row, col);
        cell.putValue(text == null ? "" : text);
        Style style = cell.getStyle();
        style.setHorizontalAlignment(center ? TextAlignmentType.CENTER : TextAlignmentType.LEFT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        cell.setStyle(style);
    }
}

