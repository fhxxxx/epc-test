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
import com.envision.epc.module.taxledger.application.dto.StampDutyDetailRowDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.StampTaxProjectLedgerSheetData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;


/**
 * 印花税明细--非2320、2355 页渲染器。
 */
@Component
public class StampTaxProjectSheetRenderer implements LedgerSheetRenderer<StampTaxProjectLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX_PROJECT;
    }

    @Override
    public void render(Workbook workbook, StampTaxProjectLedgerSheetData data, LedgerRenderContext ctx) throws Exception {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(LedgerSheetCode.STAMP_TAX_PROJECT.getSheetName());
        Cells cells = sheet.getCells();

        renderTitle(cells, data == null ? "" : data.getCompanyName());
        renderHeader(cells);
        renderRows(cells, data == null ? List.of() : data.getPayload());
        applyColumnWidths(cells);
    }

    private void renderTitle(Cells cells, String companyName) {
        Cell titleCell = cells.get(0, 0);
        titleCell.putValue("项目公司：");
        Style titleStyle = titleCell.getStyle();
        Font titleFont = titleStyle.getFont();
        titleFont.setBold(true);
        titleCell.setStyle(titleStyle);

        Cell companyCell = cells.get(0, 1);
        companyCell.putValue(companyName == null ? "" : companyName);
    }

    private void renderHeader(Cells cells) {
        String[] headers = new String[]{
                "序号", "税目", "计税依据", "税率", "应纳税额", "减免税额", "已缴税额", "应补（退）税额"
        };
        for (int col = 0; col < headers.length; col++) {
            Cell cell = cells.get(2, col);
            cell.putValue(headers[col]);
            cell.setStyle(buildHeaderStyle(cells));
        }
    }

    private void renderRows(Cells cells, List<StampDutyDetailRowDTO> rows) {
        if (rows == null) {
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            StampDutyDetailRowDTO row = rows.get(i);
            int rowIdx = 3 + i;
            boolean totalRow = row != null && "合计".equals(row.getSerialNo());

            writeTextCell(cells, rowIdx, 0, row == null ? "-" : dashIfBlank(row.getSerialNo()), totalRow, true);
            writeTextCellAllowBlank(cells, rowIdx, 1, row == null ? "" : nullToEmpty(row.getTaxItem()), totalRow, false);
            writeAmountCell(cells, rowIdx, 2, row == null ? null : row.getTaxableBasis(), totalRow);
            writeRateCell(cells, rowIdx, 3, row == null ? null : row.getTaxRate(), totalRow);
            writeAmountCell(cells, rowIdx, 4, row == null ? null : row.getTaxPayableAmount(), totalRow);
            writeAmountCell(cells, rowIdx, 5, row == null ? null : row.getTaxReductionAmount(), totalRow);
            writeAmountCell(cells, rowIdx, 6, row == null ? null : row.getTaxPaidAmount(), totalRow);
            writeAmountCell(cells, rowIdx, 7, row == null ? null : row.getTaxPayableOrRefundableAmount(), totalRow);
        }
    }

    private void writeTextCell(Cells cells, int row, int col, String value, boolean bold, boolean centered) {
        Cell cell = cells.get(row, col);
        cell.putValue(dashIfBlank(value));
        Style style = centered ? buildBodyStyleCenter(cells, bold) : buildBodyStyleLeft(cells, bold);
        cell.setStyle(style);
    }

    private void writeTextCellAllowBlank(Cells cells, int row, int col, String value, boolean bold, boolean centered) {
        Cell cell = cells.get(row, col);
        cell.putValue(nullToEmpty(value));
        Style style = centered ? buildBodyStyleCenter(cells, bold) : buildBodyStyleLeft(cells, bold);
        cell.setStyle(style);
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

    private void writeRateCell(Cells cells, int row, int col, BigDecimal value, boolean bold) {
        Cell cell = cells.get(row, col);
        Style style = buildRateStyle(cells, bold);
        if (value == null) {
            cell.putValue("-");
            style.setCustom("@");
        } else {
            cell.putValue(value.doubleValue());
        }
        cell.setStyle(style);
    }

    private void applyColumnWidths(Cells cells) {
        cells.setColumnWidth(0, 10);
        cells.setColumnWidth(1, 22);
        cells.setColumnWidth(2, 16);
        cells.setColumnWidth(3, 12);
        cells.setColumnWidth(4, 14);
        cells.setColumnWidth(5, 14);
        cells.setColumnWidth(6, 12);
        cells.setColumnWidth(7, 16);
    }

    private Style buildHeaderStyle(Cells cells) {
        Style style = cells.get(2, 0).getStyle();
        style.setPattern(BackgroundType.SOLID);
        style.setForegroundColor(Color.fromArgb(242, 188, 230));
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        Font font = style.getFont();
        font.setBold(true);
        applyBorder(style);
        return style;
    }

    private Style buildBodyStyleCenter(Cells cells, boolean bold) {
        Style style = cells.get(3, 0).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.getFont().setBold(bold);
        applyBorder(style);
        return style;
    }

    private Style buildBodyStyleLeft(Cells cells, boolean bold) {
        Style style = cells.get(3, 1).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.LEFT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.getFont().setBold(bold);
        applyBorder(style);
        return style;
    }

    private Style buildAmountStyle(Cells cells, boolean bold) {
        Style style = cells.get(3, 2).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.RIGHT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom("#,##0.00;-#,##0.00;-");
        style.getFont().setBold(bold);
        applyBorder(style);
        return style;
    }

    private Style buildRateStyle(Cells cells, boolean bold) {
        Style style = cells.get(3, 3).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom("0.0000%");
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

    private String dashIfBlank(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }
        return value;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

