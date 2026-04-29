package com.envision.epc.module.taxledger.excel;

import com.aspose.cells.Cells;
import com.aspose.cells.Name;
import com.aspose.cells.Range;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Summary 页模板渲染（Named Range 锚点）。
 */
@Slf4j
@Service
public class SummaryTemplateRenderService {

    private static final String TEMPLATE_PATH = "templates/tax-ledger/summary/tax-ledger-summary-template.xlsx";
    private static final String TEMPLATE_SHEET = "Summary_命名区域模板";
    private static final String OUTPUT_SHEET = "Summary";

    private static final String NR_STAMP_HEADER = "NR_STAMP_HEADER";
    private static final String NR_STAMP_DETAIL = "NR_STAMP_DETAIL_TMPL";
    private static final String NR_STAMP_SUBTOTAL = "NR_STAMP_SUBTOTAL_TMPL";
    private static final String NR_COMMON_HEADER = "NR_COMMON_HEADER";
    private static final String NR_COMMON_DETAIL = "NR_COMMON_DETAIL_TMPL";
    private static final String NR_COMMON_SUBTOTAL = "NR_COMMON_SUBTOTAL_TMPL";
    private static final String NR_CIT_HEADER = "NR_CIT_HEADER_TMPL";
    private static final String NR_CIT_DETAIL = "NR_CIT_DETAIL_TMPL";
    private static final String NR_CIT_SUBTOTAL = "NR_CIT_SUBTOTAL_TMPL";
    private static final String NR_FINAL_TOTAL = "NR_FINAL_TOTAL_TMPL";

    public Workbook render(SummarySheetDTO summaryData) throws Exception {
        Workbook workbook = new Workbook();
        renderInto(workbook, summaryData);
        if (workbook.getWorksheets().getCount() > 1
                && "Sheet1".equals(workbook.getWorksheets().get(0).getName())
                && workbook.getWorksheets().get(0).getCells().getMaxDataRow() < 0) {
            workbook.getWorksheets().removeAt(0);
        }
        return workbook;
    }

    public void renderInto(Workbook workbook, SummarySheetDTO summaryData) throws Exception {
        if (summaryData == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "summaryData is null");
        }
        try (InputStream templateInput = Thread.currentThread().getContextClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (templateInput == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "summary template not found: " + TEMPLATE_PATH);
            }
            Workbook templateWorkbook = new Workbook(templateInput);
            Worksheet templateSheet = templateWorkbook.getWorksheets().get(TEMPLATE_SHEET);
            if (templateSheet == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "summary template sheet not found: " + TEMPLATE_SHEET);
            }

            Worksheet summarySheet = createOutputSheet(workbook, templateSheet);
            renderSummaryRows(templateWorkbook, templateSheet, summarySheet, summaryData);
        }
    }

    private Worksheet createOutputSheet(Workbook workbook, Worksheet templateSheet) throws Exception {
        int summaryIndex = workbook.getWorksheets().add();
        Worksheet summarySheet = workbook.getWorksheets().get(summaryIndex);
        summarySheet.copy(templateSheet);
        summarySheet.setName(resolveSheetName(workbook, OUTPUT_SHEET));
        return summarySheet;
    }

    private void renderSummaryRows(Workbook templateWorkbook,
                                   Worksheet templateSheet,
                                   Worksheet summarySheet,
                                   SummarySheetDTO summaryData) throws Exception {
        Cells templateCells = templateSheet.getCells();
        Cells summaryCells = summarySheet.getCells();

        replaceGlobalTokens(summarySheet, summaryData);

        int stampHeaderRow = resolveRow(templateWorkbook, NR_STAMP_HEADER);
        int stampDetailRow = resolveRow(templateWorkbook, NR_STAMP_DETAIL);
        int stampSubtotalRow = resolveRow(templateWorkbook, NR_STAMP_SUBTOTAL);
        int commonHeaderRow = resolveRow(templateWorkbook, NR_COMMON_HEADER);
        int commonDetailRow = resolveRow(templateWorkbook, NR_COMMON_DETAIL);
        int commonSubtotalRow = resolveRow(templateWorkbook, NR_COMMON_SUBTOTAL);
        int citHeaderRow = resolveRow(templateWorkbook, NR_CIT_HEADER);
        int citDetailRow = resolveRow(templateWorkbook, NR_CIT_DETAIL);
        int citSubtotalRow = resolveRow(templateWorkbook, NR_CIT_SUBTOTAL);
        int finalTotalRow = resolveRow(templateWorkbook, NR_FINAL_TOTAL);

        int blockStart = stampHeaderRow;
        int blockEnd = finalTotalRow;
        summaryCells.deleteRows(blockStart, blockEnd - blockStart + 1, true);

        int cursor = blockStart;
        List<Integer> declaredSubtotalRows = new ArrayList<>();
        List<Integer> bookSubtotalRows = new ArrayList<>();

        SectionResult stampResult = renderSection(
                templateCells,
                summaryCells,
                cursor,
                new SectionSpec(stampHeaderRow, stampDetailRow, stampSubtotalRow, true, false),
                summaryData.getStampDutyRows(),
                this::fillStampDetailRow
        );
        cursor = stampResult.getNextCursor();
        addIfNotNull(declaredSubtotalRows, stampResult.getDeclaredSubtotalRow());

        SectionResult commonResult = renderSection(
                templateCells,
                summaryCells,
                cursor,
                new SectionSpec(commonHeaderRow, commonDetailRow, commonSubtotalRow, true, true),
                summaryData.getCommonTaxRows(),
                this::fillCommonDetailRow
        );
        cursor = commonResult.getNextCursor();
        addIfNotNull(declaredSubtotalRows, commonResult.getDeclaredSubtotalRow());
        addIfNotNull(bookSubtotalRows, commonResult.getBookSubtotalRow());

        SectionResult citResult = renderSection(
                templateCells,
                summaryCells,
                cursor,
                new SectionSpec(citHeaderRow, citDetailRow, citSubtotalRow, true, false),
                summaryData.getCorporateIncomeTaxRows(),
                this::fillCitDetailRow
        );
        cursor = citResult.getNextCursor();
        addIfNotNull(declaredSubtotalRows, citResult.getDeclaredSubtotalRow());

        insertRowCopy(summaryCells, templateCells, finalTotalRow, cursor);
        fillFinalTotal(summaryCells, cursor, summaryData.getFinalTotal(), declaredSubtotalRows, bookSubtotalRows);
        cursor++;

        log.info("summary rendered: stampRows={}, commonRows={}, citRows={}, totalRows={}",
                size(summaryData.getStampDutyRows()),
                size(summaryData.getCommonTaxRows()),
                size(summaryData.getCorporateIncomeTaxRows()),
                cursor + 1);
    }

    private <T> SectionResult renderSection(Cells templateCells,
                                            Cells summaryCells,
                                            int cursor,
                                            SectionSpec spec,
                                            List<T> rows,
                                            RowWriter<T> rowWriter) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return new SectionResult(cursor, null, null);
        }

        insertRowCopy(summaryCells, templateCells, spec.getHeaderTemplateRow(), cursor++);
        int detailStart = cursor;
        for (T row : rows) {
            insertRowCopy(summaryCells, templateCells, spec.getDetailTemplateRow(), cursor);
            rowWriter.write(summaryCells, cursor, row);
            cursor++;
        }
        int detailEnd = cursor - 1;

        insertRowCopy(summaryCells, templateCells, spec.getSubtotalTemplateRow(), cursor);
        summaryCells.get(cursor, SummaryColumnMapping.COL_TAX_BASIS_DESC).putValue("小计");

        Integer declaredSubtotalRow = null;
        Integer bookSubtotalRow = null;
        if (spec.isWithDeclaredSubtotal()) {
            setSumFormula(summaryCells, cursor, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
            declaredSubtotalRow = cursor;
        }
        if (spec.isWithBookSubtotal()) {
            setSumFormula(summaryCells, cursor, SummaryColumnMapping.COL_BOOK_AMOUNT, detailStart, detailEnd);
            bookSubtotalRow = cursor;
        }
        return new SectionResult(cursor + 1, declaredSubtotalRow, bookSubtotalRow);
    }

    private void fillStampDetailRow(Cells cells, int rowIndex, SummarySheetDTO.StampDutyItem row) {
        if (row == null) {
            return;
        }
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_SEQ, row.getSeqNo());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_TYPE, row.getTaxType());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_ITEM, row.getTaxItem());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC, row.getTaxBasisDesc());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN, row.getTaxBaseQuarter());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO, row.getLevyRatio());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE, row.getTaxRate());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, row.getActualTaxPayable());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1, row.getTaxBaseMonth1());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT, row.getTaxBaseMonth2());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT, row.getTaxBaseMonth3());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON, row.getVarianceReason());
    }

    private void fillCommonDetailRow(Cells cells, int rowIndex, SummarySheetDTO.CommonTaxItem row) {
        if (row == null) {
            return;
        }
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_SEQ, row.getSeqNo());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_TYPE, row.getTaxType());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_ITEM, row.getTaxItem());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC, row.getTaxBasisDesc());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN, row.getTaxBaseAmount());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO, row.getLevyRatio());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE, row.getTaxRate());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, row.getActualTaxPayable());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1, row.getAccountCode());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT, row.getBookAmount());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT, row.getVarianceAmount());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON, row.getVarianceReason());
    }

    private void fillCitDetailRow(Cells cells, int rowIndex, SummarySheetDTO.CorporateIncomeTaxItem row) {
        if (row == null) {
            return;
        }
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_TYPE, row.getProjectName());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_ITEM, row.getPreferentialPeriod());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC, row.getTaxableIncome());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN, row.getTaxRate());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO, row.getAnnualTaxPayable());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE, row.getQ1Tax());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, row.getQ2Tax());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1, row.getQ3Tax());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT, row.getQ4Tax());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT, row.getQ1PayLastYearQ4());
        putIfNotNull(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON, row.getLossCarryforwardUsed());
    }

    private void fillFinalTotal(Cells cells,
                                int rowIndex,
                                SummarySheetDTO.FinalTotalItem totalItem,
                                List<Integer> declaredSubtotalRows,
                                List<Integer> bookSubtotalRows) {
        String title = totalItem != null && totalItem.getTotalTitle() != null ? totalItem.getTotalTitle() : "合计";
        cells.get(rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC).putValue(title);

        if (declaredSubtotalRows.isEmpty()) {
            cells.get(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT).putValue(0);
        } else {
            cells.get(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT)
                    .setFormula(buildPlusFormula(declaredSubtotalRows, SummaryColumnMapping.COL_DECLARED_AMOUNT));
        }
        if (bookSubtotalRows.isEmpty()) {
            cells.get(rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT).putValue(0);
        } else {
            cells.get(rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT)
                    .setFormula(buildPlusFormula(bookSubtotalRows, SummaryColumnMapping.COL_BOOK_AMOUNT));
        }
    }

    private void replaceGlobalTokens(Worksheet summarySheet, SummarySheetDTO summaryData) {
        String title = buildSummaryTitle(summaryData.getCompanyName(), summaryData.getLedgerPeriod());
        String periodText = summaryData.getLedgerPeriod() == null ? "" : summaryData.getLedgerPeriod();
        replaceToken(summarySheet, "{{title}}", title);
        replaceToken(summarySheet, "{{periodText}}", periodText);
        replaceToken(summarySheet, "{{declaredTotal}}", "");
        replaceToken(summarySheet, "{{bookTotal}}", "");
    }

    private String buildSummaryTitle(String companyName, String ledgerPeriod) {
        String safeCompanyName = companyName == null ? "" : companyName.trim();
        YearMonth ym = parseYearMonth(ledgerPeriod);
        if (ym == null) {
            return safeCompanyName + "税费申报明细";
        }
        return safeCompanyName + ym.getYear() + "年" + ym.getMonthValue() + "月（税务所属期）税费申报明细";
    }

    private YearMonth parseYearMonth(String ledgerPeriod) {
        if (ledgerPeriod == null) {
            return null;
        }
        String text = ledgerPeriod.trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.matches("^\\d{6}$")) {
            text = text.substring(0, 4) + "-" + text.substring(4, 6);
        }
        if (!text.matches("^\\d{4}-\\d{2}$")) {
            return null;
        }
        try {
            return YearMonth.parse(text);
        } catch (Exception ignore) {
            return null;
        }
    }

    private void replaceToken(Worksheet sheet, String token, String replacement) {
        Cells cells = sheet.getCells();
        int maxRow = cells.getMaxRow();
        int maxCol = cells.getMaxColumn();
        for (int row = 0; row <= maxRow; row++) {
            for (int col = 0; col <= maxCol; col++) {
                String value = cells.get(row, col).getStringValue();
                if (value == null || value.isEmpty() || !value.contains(token)) {
                    continue;
                }
                cells.get(row, col).putValue(value.replace(token, replacement));
            }
        }
    }

    private int resolveRow(Workbook templateWorkbook, String name) {
        Name named = templateWorkbook.getWorksheets().getNames().get(name);
        if (named == null) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "summary named range missing, template=" + TEMPLATE_PATH + ", sheet=" + TEMPLATE_SHEET + ", namedRange=" + name);
        }
        Range range = named.getRange();
        if (range == null) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "summary named range invalid, template=" + TEMPLATE_PATH + ", sheet=" + TEMPLATE_SHEET + ", namedRange=" + name);
        }
        return range.getFirstRow();
    }

    private void insertRowCopy(Cells targetCells, Cells sourceCells, int sourceRowIndex, int destinationRowIndex) throws Exception {
        targetCells.insertRows(destinationRowIndex, 1);
        targetCells.copyRow(sourceCells, sourceRowIndex, destinationRowIndex);
    }

    private void setSumFormula(Cells cells, int rowIndex, int colIndex, int startRow, int endRow) {
        String col = toColumnName(colIndex);
        if (startRow > endRow) {
            cells.get(rowIndex, colIndex).putValue(0);
            return;
        }
        cells.get(rowIndex, colIndex).setFormula("SUM(" + col + (startRow + 1) + ":" + col + (endRow + 1) + ")");
    }

    private String buildPlusFormula(List<Integer> subtotalRows, int colIndex) {
        String col = toColumnName(colIndex);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < subtotalRows.size(); i++) {
            if (i > 0) {
                builder.append("+");
            }
            builder.append(col).append(subtotalRows.get(i) + 1);
        }
        return builder.toString();
    }

    private String toColumnName(int colIndex) {
        int index = colIndex;
        StringBuilder sb = new StringBuilder();
        while (index >= 0) {
            sb.insert(0, (char) ('A' + (index % 26)));
            index = index / 26 - 1;
        }
        return sb.toString();
    }

    private void putIfNotNull(Cells cells, int row, int col, Object value) {
        if (value == null) {
            return;
        }
        if (value instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) value;
            cells.get(row, col).putValue(bigDecimal.doubleValue());
            return;
        }
        cells.get(row, col).putValue(value);
    }

    private String resolveSheetName(Workbook workbook, String baseName) {
        if (workbook.getWorksheets().get(baseName) == null) {
            return baseName;
        }
        int idx = 1;
        while (workbook.getWorksheets().get(baseName + "_" + idx) != null) {
            idx++;
        }
        return baseName + "_" + idx;
    }

    private int size(List<?> list) {
        return list == null ? 0 : list.size();
    }

    private void addIfNotNull(List<Integer> list, Integer value) {
        if (value != null) {
            list.add(value);
        }
    }

    private interface RowWriter<T> {
        void write(Cells cells, int rowIndex, T row);
    }

    private static class SectionSpec {
        private final int headerTemplateRow;
        private final int detailTemplateRow;
        private final int subtotalTemplateRow;
        private final boolean withDeclaredSubtotal;
        private final boolean withBookSubtotal;

        private SectionSpec(int headerTemplateRow,
                            int detailTemplateRow,
                            int subtotalTemplateRow,
                            boolean withDeclaredSubtotal,
                            boolean withBookSubtotal) {
            this.headerTemplateRow = headerTemplateRow;
            this.detailTemplateRow = detailTemplateRow;
            this.subtotalTemplateRow = subtotalTemplateRow;
            this.withDeclaredSubtotal = withDeclaredSubtotal;
            this.withBookSubtotal = withBookSubtotal;
        }

        private int getHeaderTemplateRow() {
            return headerTemplateRow;
        }

        private int getDetailTemplateRow() {
            return detailTemplateRow;
        }

        private int getSubtotalTemplateRow() {
            return subtotalTemplateRow;
        }

        private boolean isWithDeclaredSubtotal() {
            return withDeclaredSubtotal;
        }

        private boolean isWithBookSubtotal() {
            return withBookSubtotal;
        }
    }

    private static class SectionResult {
        private final int nextCursor;
        private final Integer declaredSubtotalRow;
        private final Integer bookSubtotalRow;

        private SectionResult(int nextCursor, Integer declaredSubtotalRow, Integer bookSubtotalRow) {
            this.nextCursor = nextCursor;
            this.declaredSubtotalRow = declaredSubtotalRow;
            this.bookSubtotalRow = bookSubtotalRow;
        }

        private int getNextCursor() {
            return nextCursor;
        }

        private Integer getDeclaredSubtotalRow() {
            return declaredSubtotalRow;
        }

        private Integer getBookSubtotalRow() {
            return bookSubtotalRow;
        }
    }
}
