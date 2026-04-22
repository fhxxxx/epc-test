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
        if (summaryData == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "summaryData is null");
        }
        try (InputStream templateInput = Thread.currentThread().getContextClassLoader().getResourceAsStream(TEMPLATE_PATH)) {
            if (templateInput == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "summary template not found: " + TEMPLATE_PATH);
            }
            Workbook workbook = new Workbook(templateInput);
            Worksheet templateSheet = workbook.getWorksheets().get(TEMPLATE_SHEET);
            if (templateSheet == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "summary template sheet not found: " + TEMPLATE_SHEET);
            }

            int summaryIndex = workbook.getWorksheets().addCopy(templateSheet.getIndex());
            Worksheet summarySheet = workbook.getWorksheets().get(summaryIndex);
            summarySheet.setName(OUTPUT_SHEET);

            renderSummaryRows(workbook, templateSheet, summarySheet, summaryData);
            cleanupSheets(workbook, summarySheet.getName());
            return workbook;
        }
    }

    private void renderSummaryRows(Workbook workbook,
                                   Worksheet templateSheet,
                                   Worksheet summarySheet,
                                   SummarySheetDTO summaryData) throws Exception {
        Cells templateCells = templateSheet.getCells();
        Cells summaryCells = summarySheet.getCells();

        replaceGlobalTokens(summarySheet, summaryData);

        int stampHeaderRow = resolveRow(workbook, NR_STAMP_HEADER);
        int stampDetailRow = resolveRow(workbook, NR_STAMP_DETAIL);
        int stampSubtotalRow = resolveRow(workbook, NR_STAMP_SUBTOTAL);
        int commonHeaderRow = resolveRow(workbook, NR_COMMON_HEADER);
        int commonDetailRow = resolveRow(workbook, NR_COMMON_DETAIL);
        int commonSubtotalRow = resolveRow(workbook, NR_COMMON_SUBTOTAL);
        int citHeaderRow = resolveRow(workbook, NR_CIT_HEADER);
        int citDetailRow = resolveRow(workbook, NR_CIT_DETAIL);
        int citSubtotalRow = resolveRow(workbook, NR_CIT_SUBTOTAL);
        int finalTotalRow = resolveRow(workbook, NR_FINAL_TOTAL);

        int blockStart = stampHeaderRow;
        int blockEnd = finalTotalRow;
        summaryCells.deleteRows(blockStart, blockEnd - blockStart + 1, true);

        int cursor = blockStart;
        List<Integer> declaredSubtotalRows = new ArrayList<>();
        List<Integer> bookSubtotalRows = new ArrayList<>();

        cursor = renderStampSection(templateCells, summaryCells, cursor, stampHeaderRow, stampDetailRow, stampSubtotalRow,
                summaryData.getStampDutyRows(), declaredSubtotalRows);
        cursor = renderCommonSection(templateCells, summaryCells, cursor, commonHeaderRow, commonDetailRow, commonSubtotalRow,
                summaryData.getCommonTaxRows(), declaredSubtotalRows, bookSubtotalRows);
        cursor = renderCitSection(templateCells, summaryCells, cursor, citHeaderRow, citDetailRow, citSubtotalRow,
                summaryData.getCorporateIncomeTaxRows(), declaredSubtotalRows);

        insertRowCopy(summaryCells, templateCells, finalTotalRow, cursor);
        fillFinalTotal(summaryCells, cursor, summaryData.getFinalTotal(), declaredSubtotalRows, bookSubtotalRows);
        cursor++;

        log.info("summary rendered: stampRows={}, commonRows={}, citRows={}, totalRows={}",
                size(summaryData.getStampDutyRows()),
                size(summaryData.getCommonTaxRows()),
                size(summaryData.getCorporateIncomeTaxRows()),
                cursor + 1);
    }

    private int renderStampSection(Cells templateCells,
                                   Cells summaryCells,
                                   int cursor,
                                   int headerRow,
                                   int detailRow,
                                   int subtotalRow,
                                   List<SummarySheetDTO.StampDutyItem> rows,
                                   List<Integer> declaredSubtotalRows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return cursor;
        }
        insertRowCopy(summaryCells, templateCells, headerRow, cursor++);

        int detailStart = cursor;
        for (SummarySheetDTO.StampDutyItem row : rows) {
            insertRowCopy(summaryCells, templateCells, detailRow, cursor);
            fillStampDetailRow(summaryCells, cursor, row);
            cursor++;
        }
        int detailEnd = cursor - 1;

        insertRowCopy(summaryCells, templateCells, subtotalRow, cursor);
        summaryCells.get(cursor, SummaryColumnMapping.COL_TAX_BASIS_DESC).putValue("小计");
        setSumFormula(summaryCells, cursor, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
        declaredSubtotalRows.add(cursor);
        return cursor + 1;
    }

    private int renderCommonSection(Cells templateCells,
                                    Cells summaryCells,
                                    int cursor,
                                    int headerRow,
                                    int detailRow,
                                    int subtotalRow,
                                    List<SummarySheetDTO.CommonTaxItem> rows,
                                    List<Integer> declaredSubtotalRows,
                                    List<Integer> bookSubtotalRows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return cursor;
        }
        insertRowCopy(summaryCells, templateCells, headerRow, cursor++);

        int detailStart = cursor;
        for (SummarySheetDTO.CommonTaxItem row : rows) {
            insertRowCopy(summaryCells, templateCells, detailRow, cursor);
            fillCommonDetailRow(summaryCells, cursor, row);
            cursor++;
        }
        int detailEnd = cursor - 1;

        insertRowCopy(summaryCells, templateCells, subtotalRow, cursor);
        summaryCells.get(cursor, SummaryColumnMapping.COL_TAX_BASIS_DESC).putValue("小计");
        setSumFormula(summaryCells, cursor, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
        setSumFormula(summaryCells, cursor, SummaryColumnMapping.COL_BOOK_AMOUNT, detailStart, detailEnd);
        declaredSubtotalRows.add(cursor);
        bookSubtotalRows.add(cursor);
        return cursor + 1;
    }

    private int renderCitSection(Cells templateCells,
                                 Cells summaryCells,
                                 int cursor,
                                 int headerRow,
                                 int detailRow,
                                 int subtotalRow,
                                 List<SummarySheetDTO.CorporateIncomeTaxItem> rows,
                                 List<Integer> declaredSubtotalRows) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return cursor;
        }
        insertRowCopy(summaryCells, templateCells, headerRow, cursor++);

        int detailStart = cursor;
        for (SummarySheetDTO.CorporateIncomeTaxItem row : rows) {
            insertRowCopy(summaryCells, templateCells, detailRow, cursor);
            fillCitDetailRow(summaryCells, cursor, row);
            cursor++;
        }
        int detailEnd = cursor - 1;

        insertRowCopy(summaryCells, templateCells, subtotalRow, cursor);
        summaryCells.get(cursor, SummaryColumnMapping.COL_TAX_BASIS_DESC).putValue("小计");
        setSumFormula(summaryCells, cursor, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
        declaredSubtotalRows.add(cursor);
        return cursor + 1;
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
            cells.get(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT).setFormula(buildPlusFormula(declaredSubtotalRows, SummaryColumnMapping.COL_DECLARED_AMOUNT));
        }
        if (bookSubtotalRows.isEmpty()) {
            BigDecimal fallback = totalItem == null ? null : totalItem.getBookTotal();
            cells.get(rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT).putValue(fallback == null ? 0 : fallback.doubleValue());
        } else {
            cells.get(rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT).setFormula(buildPlusFormula(bookSubtotalRows, SummaryColumnMapping.COL_BOOK_AMOUNT));
        }
    }

    private void replaceGlobalTokens(Worksheet summarySheet, SummarySheetDTO summaryData) {
        String title = (summaryData.getCompanyName() == null ? "" : summaryData.getCompanyName()) + " 税务台账";
        String periodText = summaryData.getLedgerPeriod() == null ? "" : summaryData.getLedgerPeriod();
        replaceToken(summarySheet, "{{title}}", title);
        replaceToken(summarySheet, "{{periodText}}", periodText);
        replaceToken(summarySheet, "{{declaredTotal}}", "");
        replaceToken(summarySheet, "{{bookTotal}}", "");
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

    private int resolveRow(Workbook workbook, String name) {
        Name named = workbook.getWorksheets().getNames().get(name);
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
        if (value instanceof BigDecimal bigDecimal) {
            cells.get(row, col).putValue(bigDecimal.doubleValue());
            return;
        }
        cells.get(row, col).putValue(value);
    }

    private void cleanupSheets(Workbook workbook, String keepSheetName) {
        for (int i = workbook.getWorksheets().getCount() - 1; i >= 0; i--) {
            Worksheet ws = workbook.getWorksheets().get(i);
            if (keepSheetName.equals(ws.getName())) {
                continue;
            }
            workbook.getWorksheets().removeAt(i);
        }
    }

    private int size(List<?> list) {
        return list == null ? 0 : list.size();
    }
}
