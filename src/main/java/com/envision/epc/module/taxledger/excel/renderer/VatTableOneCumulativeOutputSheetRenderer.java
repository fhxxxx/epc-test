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
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.VatTableOneCumulativeOutputLedgerSheetData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


/**
 * 增值税表一 累计销项-2320、2355 页渲染器。
 */
@Component
public class VatTableOneCumulativeOutputSheetRenderer implements LedgerSheetRenderer<VatTableOneCumulativeOutputLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT;
    }

    @Override
    public void render(Workbook workbook, VatTableOneCumulativeOutputLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT.getSheetName());
        Cells cells = sheet.getCells();

        List<VatTableOneCumulativeOutputItemDTO> rows = data == null || data.getPayload() == null ? List.of() : data.getPayload();
        renderHeaders(cells);
        renderRows(cells, rows);
        mergePeriodColumn(cells, rows);
        applyColumnWidths(cells);
    }

    private void renderHeaders(Cells cells) {
        cells.get(1, 0).putValue("所属期");
        cells.get(1, 1).putValue("涉税类别");
        cells.get(1, 2).putValue("已开票");
        cells.get(1, 5).putValue("未开票");
        cells.get(1, 8).putValue("合计");

        cells.get(2, 2).putValue("不含税金额");
        cells.get(2, 3).putValue("税额");
        cells.get(2, 4).putValue("税率");
        cells.get(2, 5).putValue("不含税金额");
        cells.get(2, 6).putValue("税额");
        cells.get(2, 7).putValue("税率");
        cells.get(2, 8).putValue("不含税金额");
        cells.get(2, 9).putValue("税额");

        cells.merge(1, 0, 2, 1); // A2:A3
        cells.merge(1, 1, 2, 1); // B2:B3
        cells.merge(1, 2, 1, 3); // C2:E2
        cells.merge(1, 5, 1, 3); // F2:H2
        cells.merge(1, 8, 1, 2); // I2:J2

        Style headerStyle = buildHeaderStyle(cells);
        for (int r = 1; r <= 2; r++) {
            for (int c = 0; c <= 9; c++) {
                cells.get(r, c).setStyle(headerStyle);
            }
        }
    }

    private void renderRows(Cells cells, List<VatTableOneCumulativeOutputItemDTO> rows) {
        int startRow = 3; // 第4行开始写数据
        for (int i = 0; i < rows.size(); i++) {
            VatTableOneCumulativeOutputItemDTO row = rows.get(i);
            int r = startRow + i;
            writeTextCell(cells, r, 0, row == null ? null : row.getPeriod(), true);
            writeTextCell(cells, r, 1, row == null ? null : row.getTaxCategory(), false);
            writeAmountCell(cells, r, 2, row == null ? null : row.getInvoicedAmountExclTax());
            writeAmountCell(cells, r, 3, row == null ? null : row.getInvoicedTaxAmount());
            writeRateCell(cells, r, 4, row == null ? null : row.getInvoicedTaxRate());
            writeAmountCell(cells, r, 5, row == null ? null : row.getUninvoicedAmountExclTax());
            writeAmountCell(cells, r, 6, row == null ? null : row.getUninvoicedTaxAmount());
            writeRateCell(cells, r, 7, row == null ? null : row.getUninvoicedTaxRate());
            writeAmountCell(cells, r, 8, row == null ? null : row.getTotalAmountExclTax());
            writeAmountCell(cells, r, 9, row == null ? null : row.getTotalTaxAmount());
        }
    }

    private void mergePeriodColumn(Cells cells, List<VatTableOneCumulativeOutputItemDTO> rows) {
        if (rows.isEmpty()) {
            return;
        }
        int startRow = 3;
        int groupStart = 0;
        while (groupStart < rows.size()) {
            String period = safeText(rows.get(groupStart) == null ? null : rows.get(groupStart).getPeriod());
            int groupEnd = groupStart;
            while (groupEnd + 1 < rows.size()) {
                String next = safeText(rows.get(groupEnd + 1) == null ? null : rows.get(groupEnd + 1).getPeriod());
                if (!period.equals(next)) {
                    break;
                }
                groupEnd++;
            }
            int count = groupEnd - groupStart + 1;
            if (count > 1) {
                cells.merge(startRow + groupStart, 0, count, 1);
            }
            groupStart = groupEnd + 1;
        }
    }

    private void writeTextCell(Cells cells, int row, int col, String value, boolean center) {
        Cell cell = cells.get(row, col);
        cell.putValue(isBlank(value) ? "-" : value);
        cell.setStyle(center ? buildBodyTextCenterStyle(cells) : buildBodyTextLeftStyle(cells));
    }

    private void writeAmountCell(Cells cells, int row, int col, BigDecimal value) {
        Cell cell = cells.get(row, col);
        Style style = buildBodyAmountStyle(cells);
        if (value == null) {
            cell.putValue("-");
            style.setCustom("@");
        } else {
            cell.putValue(value.doubleValue());
        }
        cell.setStyle(style);
    }

    private void writeRateCell(Cells cells, int row, int col, BigDecimal value) {
        Cell cell = cells.get(row, col);
        Style style = buildBodyRateStyle(cells);
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
        cells.setColumnWidth(1, 26);
        cells.setColumnWidth(2, 16);
        cells.setColumnWidth(3, 14);
        cells.setColumnWidth(4, 10);
        cells.setColumnWidth(5, 16);
        cells.setColumnWidth(6, 14);
        cells.setColumnWidth(7, 10);
        cells.setColumnWidth(8, 16);
        cells.setColumnWidth(9, 14);
    }

    private Style buildHeaderStyle(Cells cells) {
        Style style = cells.get(1, 0).getStyle();
        style.setPattern(BackgroundType.SOLID);
        style.setForegroundColor(Color.fromArgb(242, 188, 230));
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        Font font = style.getFont();
        font.setBold(true);
        applyBorder(style);
        return style;
    }

    private Style buildBodyTextCenterStyle(Cells cells) {
        Style style = cells.get(3, 0).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        applyBorder(style);
        return style;
    }

    private Style buildBodyTextLeftStyle(Cells cells) {
        Style style = cells.get(3, 1).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.LEFT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        applyBorder(style);
        return style;
    }

    private Style buildBodyAmountStyle(Cells cells) {
        Style style = cells.get(3, 2).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.RIGHT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom("#,##0.00;-#,##0.00;-");
        applyBorder(style);
        return style;
    }

    private Style buildBodyRateStyle(Cells cells) {
        Style style = cells.get(3, 4).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom("0.00%");
        applyBorder(style);
        return style;
    }

    private void applyBorder(Style style) {
        style.getBorders().getByBorderType(BorderType.TOP_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.BOTTOM_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.LEFT_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.RIGHT_BORDER).setLineStyle(CellBorderType.THIN);
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}

