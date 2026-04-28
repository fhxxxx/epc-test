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
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.UninvoicedMonitorLedgerSheetData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


/**
 * 未开票数监控 页渲染器。
 */
@Component
public class UninvoicedMonitorSheetRenderer implements LedgerSheetRenderer<UninvoicedMonitorLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.UNINVOICED_MONITOR;
    }

    @Override
    public void render(Workbook workbook, UninvoicedMonitorLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(LedgerSheetCode.UNINVOICED_MONITOR.getSheetName());
        Cells cells = sheet.getCells();

        mergeHeader(cells);
        writeHeader(cells);
        writeRows(cells, data == null ? List.of() : data.getRows());
        applyColumnWidths(cells);
    }

    private void mergeHeader(Cells cells) {
        cells.merge(0, 0, 2, 1);
        cells.merge(0, 1, 1, 4);
        cells.merge(0, 5, 1, 2);
        cells.merge(0, 7, 1, 2);
    }

    private void writeHeader(Cells cells) {
        writeHeaderCell(cells, 0, 0, "所属期");
        writeHeaderCell(cells, 0, 1, "申报账面数");
        writeHeaderCell(cells, 0, 5, "开票数");
        writeHeaderCell(cells, 0, 7, "未开票数");

        writeHeaderCell(cells, 1, 1, "营业收入");
        writeHeaderCell(cells, 1, 2, "利息收入");
        writeHeaderCell(cells, 1, 3, "其他收益");
        writeHeaderCell(cells, 1, 4, "销项税额");
        writeHeaderCell(cells, 1, 5, "销售收入");
        writeHeaderCell(cells, 1, 6, "销项税额");
        writeHeaderCell(cells, 1, 7, "销售收入");
        writeHeaderCell(cells, 1, 8, "销项税额");
    }

    private void writeRows(Cells cells, List<UninvoicedMonitorItemDTO> rows) {
        for (int i = 0; i < rows.size(); i++) {
            UninvoicedMonitorItemDTO row = rows.get(i);
            int r = 2 + i;
            boolean total = row != null && "合计数".equals(row.getPeriod());
            writePeriodCell(cells, r, 0, row == null ? "" : row.getPeriod(), total);
            writeAmountCell(cells, r, 1, row == null ? null : row.getDeclaredMainBusinessRevenue(), total);
            writeAmountCell(cells, r, 2, row == null ? null : row.getDeclaredInterestIncome(), total);
            writeAmountCell(cells, r, 3, row == null ? null : row.getDeclaredOtherIncome(), total);
            writeAmountCell(cells, r, 4, row == null ? null : row.getDeclaredOutputTax(), total);
            writeAmountCell(cells, r, 5, row == null ? null : row.getInvoicedSalesIncome(), total);
            writeAmountCell(cells, r, 6, row == null ? null : row.getInvoicedOutputTax(), total);
            writeAmountCell(cells, r, 7, row == null ? null : row.getUninvoicedSalesIncome(), total);
            writeAmountCell(cells, r, 8, row == null ? null : row.getUninvoicedOutputTax(), total);
        }
    }

    private void writeHeaderCell(Cells cells, int row, int col, String value) {
        Cell cell = cells.get(row, col);
        cell.putValue(value);
        cell.setStyle(buildHeaderStyle(cells));
    }

    private void writePeriodCell(Cells cells, int row, int col, String value, boolean bold) {
        Cell cell = cells.get(row, col);
        cell.putValue(value == null ? "" : value);
        cell.setStyle(buildPeriodStyle(cells, bold));
    }

    private void writeAmountCell(Cells cells, int row, int col, BigDecimal value, boolean bold) {
        Cell cell = cells.get(row, col);
        Style style = buildAmountStyle(cells, bold);
        if (value == null) {
            cell.putValue("-");
            style.setCustom("@");
        } else {
            cell.putValue(value.doubleValue());
        }
        cell.setStyle(style);
    }

    private void applyColumnWidths(Cells cells) {
        cells.setColumnWidth(0, 12);
        for (int col = 1; col <= 8; col++) {
            cells.setColumnWidth(col, 16);
        }
    }

    private Style buildHeaderStyle(Cells cells) {
        Style style = cells.get(0, 0).getStyle();
        style.setPattern(BackgroundType.SOLID);
        style.setForegroundColor(Color.fromArgb(242, 242, 242));
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        Font font = style.getFont();
        font.setBold(true);
        applyBorder(style);
        return style;
    }

    private Style buildPeriodStyle(Cells cells, boolean bold) {
        Style style = cells.get(2, 0).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.getFont().setBold(bold);
        applyBorder(style);
        return style;
    }

    private Style buildAmountStyle(Cells cells, boolean bold) {
        Style style = cells.get(2, 1).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.RIGHT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom("#,##0.00;-#,##0.00;-");
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
}

