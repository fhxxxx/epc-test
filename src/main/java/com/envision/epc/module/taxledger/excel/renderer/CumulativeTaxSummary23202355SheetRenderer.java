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
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeTaxSummary23202355LedgerSheetData;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

/**
 * 累计税金汇总表-2320、2355 页渲染器。
 */
@Component
public class CumulativeTaxSummary23202355SheetRenderer implements LedgerSheetRenderer<CumulativeTaxSummary23202355LedgerSheetData> {
    private static final String ACCOUNTING_NUMBER_FORMAT = "_(* #,##0.00_);_(* (#,##0.00);_(* \"-\"??_);_(@_)";
    private static final int HEADER_ROW = 0;
    private static final int DATA_START_ROW = 1;
    private static final int METRIC_COL = 0;
    private static final int FIRST_PERIOD_COL = 1;

    private static final List<MetricDef> METRICS = List.of(
            new MetricDef("账面收入", CumulativeTaxSummary23202355ColumnDTO::getBookIncome),
            new MetricDef("其他收益", CumulativeTaxSummary23202355ColumnDTO::getOtherIncome),
            new MetricDef("项目开票", CumulativeTaxSummary23202355ColumnDTO::getProjectInvoicing),
            new MetricDef("利息开票", CumulativeTaxSummary23202355ColumnDTO::getInterestInvoicing),
            new MetricDef("销项税额(A)", CumulativeTaxSummary23202355ColumnDTO::getOutputTaxA),
            new MetricDef("本期进项税额(B)", CumulativeTaxSummary23202355ColumnDTO::getCurrentInputTaxB),
            new MetricDef("上期留抵税额(C)", CumulativeTaxSummary23202355ColumnDTO::getOpeningRetainedInputTaxC),
            new MetricDef("进项税额转出(D)", CumulativeTaxSummary23202355ColumnDTO::getInputTaxTransferOutD),
            new MetricDef("异地预缴税增值税(E)", CumulativeTaxSummary23202355ColumnDTO::getRemotePrepaidVatE),
            new MetricDef("应纳增值税额(A-B-C+D-E)", CumulativeTaxSummary23202355ColumnDTO::getVatPayableAminusBminusCplusDminusE),
            new MetricDef("增值税", CumulativeTaxSummary23202355ColumnDTO::getVatAmount),
            new MetricDef("印花税", CumulativeTaxSummary23202355ColumnDTO::getStampDuty),
            new MetricDef("城建税", CumulativeTaxSummary23202355ColumnDTO::getUrbanConstructionTax),
            new MetricDef("教育费附加", CumulativeTaxSummary23202355ColumnDTO::getEducationSurcharge),
            new MetricDef("地方教育费附加", CumulativeTaxSummary23202355ColumnDTO::getLocalEducationSurcharge),
            new MetricDef("房产税", CumulativeTaxSummary23202355ColumnDTO::getPropertyTax),
            new MetricDef("城镇土地使用", CumulativeTaxSummary23202355ColumnDTO::getUrbanLandUseTax),
            new MetricDef("企业所得税", CumulativeTaxSummary23202355ColumnDTO::getCorporateIncomeTax),
            new MetricDef("个税", CumulativeTaxSummary23202355ColumnDTO::getIndividualIncomeTax),
            new MetricDef("残疾人保障金", CumulativeTaxSummary23202355ColumnDTO::getDisabledPersonsEmploymentSecurityFund),
            new MetricDef("合计", CumulativeTaxSummary23202355ColumnDTO::getTotalTaxAmount)
    );

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355;
    }

    @Override
    public void render(Workbook workbook, CumulativeTaxSummary23202355LedgerSheetData data, LedgerRenderContext ctx) {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355.getSheetName());
        Cells cells = sheet.getCells();

        List<CumulativeTaxSummary23202355ColumnDTO> columns = data == null || data.getPayload() == null ? List.of() : data.getPayload();
        writeHeader(cells, columns);
        writeMetrics(cells, columns);
        applyColumnWidth(cells, columns.size());
    }

    private void writeHeader(Cells cells, List<CumulativeTaxSummary23202355ColumnDTO> columns) {
        writeHeaderCell(cells, HEADER_ROW, METRIC_COL, "指标");
        for (int i = 0; i < columns.size(); i++) {
            CumulativeTaxSummary23202355ColumnDTO column = columns.get(i);
            writeHeaderCell(cells, HEADER_ROW, FIRST_PERIOD_COL + i, normalizePeriod(column == null ? null : column.getPeriod()));
        }
    }

    private void writeMetrics(Cells cells, List<CumulativeTaxSummary23202355ColumnDTO> columns) {
        for (int rowOffset = 0; rowOffset < METRICS.size(); rowOffset++) {
            int row = DATA_START_ROW + rowOffset;
            MetricDef metric = METRICS.get(rowOffset);
            writeMetricNameCell(cells, row, metric.name());
            for (int i = 0; i < columns.size(); i++) {
                CumulativeTaxSummary23202355ColumnDTO column = columns.get(i);
                BigDecimal value = column == null ? null : metric.valueReader().apply(column);
                writeAmountCell(cells, row, FIRST_PERIOD_COL + i, value);
            }
        }
    }

    private void writeHeaderCell(Cells cells, int row, int col, String value) {
        Cell cell = cells.get(row, col);
        cell.putValue(value == null ? "" : value);
        cell.setStyle(buildHeaderStyle(cells));
    }

    private void writeMetricNameCell(Cells cells, int row, String value) {
        Cell cell = cells.get(row, METRIC_COL);
        cell.putValue(value == null ? "" : value);
        cell.setStyle(buildMetricNameStyle(cells));
    }

    private void writeAmountCell(Cells cells, int row, int col, BigDecimal value) {
        Cell cell = cells.get(row, col);
        if (value != null) {
            cell.putValue(value.doubleValue());
        }
        cell.setStyle(buildAmountStyle(cells));
    }

    private void applyColumnWidth(Cells cells, int periodCount) {
        cells.setColumnWidth(METRIC_COL, 28);
        for (int i = 0; i < periodCount; i++) {
            cells.setColumnWidth(FIRST_PERIOD_COL + i, 16);
        }
    }

    private Style buildHeaderStyle(Cells cells) {
        Style style = cells.get(HEADER_ROW, METRIC_COL).getStyle();
        style.setPattern(BackgroundType.SOLID);
        style.setForegroundColor(Color.fromArgb(242, 242, 242));
        style.setHorizontalAlignment(TextAlignmentType.CENTER);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        Font font = style.getFont();
        font.setBold(true);
        applyBorder(style);
        return style;
    }

    private Style buildMetricNameStyle(Cells cells) {
        Style style = cells.get(DATA_START_ROW, METRIC_COL).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.LEFT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        applyBorder(style);
        return style;
    }

    private Style buildAmountStyle(Cells cells) {
        Style style = cells.get(DATA_START_ROW, FIRST_PERIOD_COL).getStyle();
        style.setHorizontalAlignment(TextAlignmentType.RIGHT);
        style.setVerticalAlignment(TextAlignmentType.CENTER);
        style.setCustom(ACCOUNTING_NUMBER_FORMAT);
        applyBorder(style);
        return style;
    }

    private void applyBorder(Style style) {
        style.getBorders().getByBorderType(BorderType.TOP_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.BOTTOM_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.LEFT_BORDER).setLineStyle(CellBorderType.THIN);
        style.getBorders().getByBorderType(BorderType.RIGHT_BORDER).setLineStyle(CellBorderType.THIN);
    }

    private String normalizePeriod(String period) {
        if (period == null) {
            return "";
        }
        String text = period.trim();
        if (text.matches("^\\d{4}-\\d{2}$")) {
            return text.replace("-", "");
        }
        return text;
    }

    private record MetricDef(String name, Function<CumulativeTaxSummary23202355ColumnDTO, BigDecimal> valueReader) {
    }
}

