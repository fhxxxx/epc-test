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
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativePaymentLedgerSheetData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


/**
 * 项目累计缴纳 页渲染器。
 */
@Component
public class ProjectCumulativePaymentSheetRenderer implements LedgerSheetRenderer<ProjectCumulativePaymentLedgerSheetData> {
    private static final String ACCOUNTING_NUMBER_FORMAT = "_(* #,##0.00_);_(* (#,##0.00);_(* \"-\"??_);_(@_)";

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT;
    }

    @Override
    public void render(Workbook workbook, ProjectCumulativePaymentLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT.getSheetName());
        Cells cells = sheet.getCells();

        List<String> taxHeaders = data == null || data.getTaxHeaders() == null ? List.of() : data.getTaxHeaders();
        List<ProjectCumulativePaymentLedgerSheetData.RowData> monthRows =
                data == null || data.getMonthRows() == null ? List.of() : data.getMonthRows();

        int colA = 0;
        int colPeriod = 1;
        int colTaxStart = 2;
        int colTaxEnd = colTaxStart + Math.max(taxHeaders.size() - 1, 0);
        int colPaid = colTaxStart + taxHeaders.size();
        int colDeclared = colPaid + 1;
        int colDiff = colPaid + 2;
        int colReason = colPaid + 3;

        writeHeader(cells, data, taxHeaders, colPeriod, colTaxStart, colTaxEnd, colPaid, colDeclared, colDiff, colReason);
        writeRows(cells, monthRows, data == null ? null : data.getTotalRow(),
                taxHeaders, colA, colPeriod, colTaxStart, colTaxEnd, colPaid, colDeclared, colDiff, colReason);
        applyColumnWidth(cells, colA, colPeriod, colTaxStart, colTaxEnd, colPaid, colDeclared, colDiff, colReason);
    }

    private void writeHeader(Cells cells,
                             ProjectCumulativePaymentLedgerSheetData data,
                             List<String> taxHeaders,
                             int colPeriod,
                             int colTaxStart,
                             int colTaxEnd,
                             int colPaid,
                             int colDeclared,
                             int colDiff,
                             int colReason) {
        String title = (data == null ? "" : data.getLedgerYear()) + "年项目累计缴纳";
        int leftHeaderWidth = colPaid - colPeriod + 1;

        cells.merge(0, colPeriod, 1, leftHeaderWidth);
        writeHeaderCell(cells, 0, colPeriod, title, true);

        cells.merge(1, colPeriod, 2, 1);
        writeHeaderCell(cells, 1, colPeriod, "实际缴纳所属期", true);

        if (!taxHeaders.isEmpty()) {
            cells.merge(1, colTaxStart, 1, colTaxEnd - colTaxStart + 1);
            writeHeaderCell(cells, 1, colTaxStart, "税种", true);
        }
        cells.merge(1, colPaid, 2, 1);
        writeHeaderCell(cells, 1, colPaid, "实缴税金", true);

        cells.merge(1, colDeclared, 2, 1);
        writeHeaderCell(cells, 1, colDeclared, "申请税金", true);
        cells.merge(1, colDiff, 2, 1);
        writeHeaderCell(cells, 1, colDiff, "差异", true);
        cells.merge(1, colReason, 2, 1);
        writeHeaderCell(cells, 1, colReason, "原因", true);

        for (int i = 0; i < taxHeaders.size(); i++) {
            writeHeaderCell(cells, 2, colTaxStart + i, taxHeaders.get(i), false);
        }
    }

    private void writeRows(Cells cells,
                           List<ProjectCumulativePaymentLedgerSheetData.RowData> monthRows,
                           ProjectCumulativePaymentLedgerSheetData.RowData totalRow,
                           List<String> taxHeaders,
                           int colA,
                           int colPeriod,
                           int colTaxStart,
                           int colTaxEnd,
                           int colPaid,
                           int colDeclared,
                           int colDiff,
                           int colReason) {
        int startRow = 3;
        int rowIndex = startRow;
        for (ProjectCumulativePaymentLedgerSheetData.RowData rowData : monthRows) {
            writeSingleRow(cells, rowData, taxHeaders, rowIndex,
                    colA, colPeriod, colTaxStart, colTaxEnd, colPaid, colDeclared, colDiff, colReason, false, startRow);
            rowIndex++;
        }
        if (totalRow != null) {
            writeSingleRow(cells, totalRow, taxHeaders, rowIndex,
                    colA, colPeriod, colTaxStart, colTaxEnd, colPaid, colDeclared, colDiff, colReason, true, startRow);
        }
    }

    private void writeSingleRow(Cells cells,
                                ProjectCumulativePaymentLedgerSheetData.RowData rowData,
                                List<String> taxHeaders,
                                int row,
                                int colA,
                                int colPeriod,
                                int colTaxStart,
                                int colTaxEnd,
                                int colPaid,
                                int colDeclared,
                                int colDiff,
                                int colReason,
                                boolean total,
                                int monthStartRow) {
        writeTextCell(cells, row, colA, "", total);
        writeTextCell(cells, row, colPeriod, rowData == null ? "" : rowData.getPeriodLabel(), total);

        for (int i = 0; i < taxHeaders.size(); i++) {
            String header = taxHeaders.get(i);
            BigDecimal value = rowData == null || rowData.getValuesByTaxHeader() == null ? null : rowData.getValuesByTaxHeader().get(header);
            writeAmountCell(cells, row, colTaxStart + i, value, total);
        }

        String paidRef = cellRef(row, colPaid);
        String declaredRef = cellRef(row, colDeclared);
        if (total) {
            writeAmountFormula(cells, row, colPaid, "SUM(" + cellRef(monthStartRow, colPaid) + ":" + cellRef(row - 1, colPaid) + ")", true);
            writeAmountFormula(cells, row, colDeclared, "SUM(" + cellRef(monthStartRow, colDeclared) + ":" + cellRef(row - 1, colDeclared) + ")", true);
            writeAmountFormula(cells, row, colDiff, "SUM(" + cellRef(monthStartRow, colDiff) + ":" + cellRef(row - 1, colDiff) + ")", true);
            writeTextCell(cells, row, colReason, "", true);
            return;
        }

        if (colTaxEnd >= colTaxStart) {
            writeAmountFormula(cells, row, colPaid, "SUM(" + cellRef(row, colTaxStart) + ":" + cellRef(row, colTaxEnd) + ")", false);
        } else {
            writeAmountFormula(cells, row, colPaid, "0", false);
        }
        writeAmountCell(cells, row, colDeclared, rowData == null ? null : rowData.getDeclaredTotal(), false);
        writeAmountFormula(cells, row, colDiff, paidRef + "-" + declaredRef, false);
        writeTextCell(cells, row, colReason, rowData == null ? "" : rowData.getReason(), false);
    }

    private void writeHeaderCell(Cells cells, int row, int col, String value, boolean topHeader) {
        Cell cell = cells.get(row, col);
        cell.putValue(value == null ? "" : value);
        cell.setStyle(buildHeaderStyle(cells, topHeader));
    }

    private void writeTextCell(Cells cells, int row, int col, String value, boolean bold) {
        Cell cell = cells.get(row, col);
        cell.putValue(value == null ? "" : value);
        cell.setStyle(buildTextStyle(cells, bold));
    }

    private void writeAmountCell(Cells cells, int row, int col, BigDecimal value, boolean bold) {
        Cell cell = cells.get(row, col);
        if (value != null) {
            cell.putValue(value.doubleValue());
        }
        cell.setStyle(buildAmountStyle(cells, bold));
    }

    private void writeAmountFormula(Cells cells, int row, int col, String formula, boolean bold) {
        Cell cell = cells.get(row, col);
        cell.setFormula(formula);
        cell.setStyle(buildAmountStyle(cells, bold));
    }

    private void applyColumnWidth(Cells cells,
                                  int colA,
                                  int colPeriod,
                                  int colTaxStart,
                                  int colTaxEnd,
                                  int colPaid,
                                  int colDeclared,
                                  int colDiff,
                                  int colReason) {
        cells.setColumnWidth(colA, 3);
        cells.setColumnWidth(colPeriod, 16);
        if (colTaxEnd >= colTaxStart) {
            for (int c = colTaxStart; c <= colTaxEnd; c++) {
                cells.setColumnWidth(c, 15);
            }
        }
        cells.setColumnWidth(colPaid, 16);
        cells.setColumnWidth(colDeclared, 16);
        cells.setColumnWidth(colDiff, 16);
        cells.setColumnWidth(colReason, 20);
    }

    private Style buildHeaderStyle(Cells cells, boolean topHeader) {
        Style style = cells.get(0, 1).getStyle();
        style.setPattern(BackgroundType.SOLID);
        style.setForegroundColor(Color.fromArgb(242, 242, 242));
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        Font font = style.getFont();
        font.setBold(true);
        if (topHeader) {
            font.setSize(12);
        }
        applyBorder(style);
        return style;
    }

    private Style buildTextStyle(Cells cells, boolean bold) {
        Style style = cells.get(3, 1).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.getFont().setBold(bold);
        applyBorder(style);
        return style;
    }

    private Style buildAmountStyle(Cells cells, boolean bold) {
        Style style = cells.get(3, 2).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.RIGHT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom(ACCOUNTING_NUMBER_FORMAT);
        style.getFont().setBold(bold);
        applyBorder(style);
        return style;
    }

    private void applyBorder(Style style) {
        style.getBorders().getByBorderType(BorderType.TOP_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.BOTTOM_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.LEFT_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.RIGHT_BORDER).setLineStyle(CellBorderType.THIN);
    }

    private String cellRef(int row, int col) {
        return columnName(col) + (row + 1);
    }

    private String columnName(int col) {
        int v = col + 1;
        StringBuilder sb = new StringBuilder();
        while (v > 0) {
            int rem = (v - 1) % 26;
            sb.insert(0, (char) ('A' + rem));
            v = (v - 1) / 26;
        }
        return sb.toString();
    }
}

