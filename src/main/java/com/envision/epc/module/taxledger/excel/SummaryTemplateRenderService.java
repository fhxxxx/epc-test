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
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Summary 页模板渲染（Named Range 锚点）。
 */
@Slf4j
@Service
public class SummaryTemplateRenderService {
    private static final Pattern LEADING_INTEGER = Pattern.compile("^\\s*(\\d+)\\b.*$");

    private static final String TEMPLATE_PATH = "templates/tax-ledger/summary/tax-ledger-summary-template.xlsx";
    private static final String TEMPLATE_SHEET = "Summary_命名区域模板";
    private static final String OUTPUT_SHEET = "Summary";

    private static final String NR_STAMP_HEADER = "NR_STAMP_HEADER";
    private static final String NR_STAMP_DETAIL = "NR_STAMP_DETAIL_TMPL";
    private static final String NR_STAMP_SUBTOTAL = "NR_STAMP_SUBTOTAL_TMPL";
    private static final String NR_VAT_HEADER = "NR_VAT_HEADER";
    private static final String NR_VAT_DETAIL = "NR_VAT_DETAIL_TMPL";
    private static final String NR_VAT_SUBTOTAL = "NR_VAT_SUBTOTAL_TMPL";
    private static final String NR_COMMON_HEADER = "NR_COMMON_HEADER";
    private static final String NR_COMMON_DETAIL = "NR_COMMON_DETAIL_TMPL";
    private static final String NR_COMMON_SUBTOTAL = "NR_COMMON_SUBTOTAL_TMPL";
    private static final String NR_CIT_HEADER = "NR_CIT_HEADER_TMPL";
    private static final String NR_CIT_DETAIL = "NR_CIT_DETAIL_TMPL";
    private static final String NR_CIT_SUBTOTAL = "NR_CIT_SUBTOTAL_TMPL";
    private static final String NR_FINAL_TOTAL = "NR_FINAL_TOTAL_TMPL";
    private static final String VAT_CHANGE_SHEET_NAME = "增值税变动表";
    private static final String VAT_ITEM_HEADER = "条目";
    private static final String VAT_TOTAL_HEADER = "合计";
    private static final String VAT_ITEM_FIXED_ASSET_DISPOSAL = "销项税额-固定资产处置收入";
    private static final String VAT_ITEM_FINANCE_INTEREST = "销项税额-财务费用-利息收入";
    private static final String VAT_ITEM_OTHER_INCOME = "销项税额-其他收益";

    private static final SummaryTemplateRowSpec STAMP_HEADER_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.STAMP_SECTION_HEADER,
            NR_STAMP_HEADER,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec STAMP_DETAIL_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.STAMP_DETAIL_ROW,
            NR_STAMP_DETAIL,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec STAMP_SUBTOTAL_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.STAMP_SUBTOTAL_ROW,
            NR_STAMP_SUBTOTAL,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec VAT_HEADER_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.VAT_SECTION_HEADER,
            NR_VAT_HEADER,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec VAT_DETAIL_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.VAT_DETAIL_ROW,
            NR_VAT_DETAIL,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec VAT_SUBTOTAL_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.VAT_SUBTOTAL_ROW,
            NR_VAT_SUBTOTAL,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec CIT_HEADER_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.CIT_SECTION_HEADER,
            NR_CIT_HEADER,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec CIT_DETAIL_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.CIT_DETAIL_ROW,
            NR_CIT_DETAIL,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );
    private static final SummaryTemplateRowSpec CIT_SUBTOTAL_SPEC = SummaryTemplateRowSpec.of(
            SummaryTemplateNamespace.CIT_SUBTOTAL_ROW,
            NR_CIT_SUBTOTAL,
            SummaryColumnMapping.COL_SEQ,
            SummaryColumnMapping.COL_VARIANCE_REASON
    );

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
            renderSummaryRows(workbook, templateWorkbook, templateSheet, summarySheet, summaryData);
        }
    }

    private Worksheet createOutputSheet(Workbook workbook, Worksheet templateSheet) throws Exception {
        int summaryIndex = workbook.getWorksheets().add();
        Worksheet summarySheet = workbook.getWorksheets().get(summaryIndex);
        summarySheet.copy(templateSheet);
        summarySheet.setName(resolveSheetName(workbook, OUTPUT_SHEET));
        return summarySheet;
    }

    private void renderSummaryRows(Workbook outputWorkbook,
                                   Workbook templateWorkbook,
                                   Worksheet templateSheet,
                                   Worksheet summarySheet,
                                   SummarySheetDTO summaryData) throws Exception {
        Cells templateCells = templateSheet.getCells();
        Cells summaryCells = summarySheet.getCells();

        replaceGlobalTokens(summarySheet, summaryData);
        SummaryTemplateStyleRegistry stampStyleRegistry = SummaryTemplateStyleRegistry.fromTemplate(
                templateWorkbook,
                TEMPLATE_PATH,
                TEMPLATE_SHEET,
                STAMP_HEADER_SPEC,
                STAMP_DETAIL_SPEC,
                STAMP_SUBTOTAL_SPEC,
                VAT_HEADER_SPEC,
                VAT_DETAIL_SPEC,
                VAT_SUBTOTAL_SPEC,
                CIT_HEADER_SPEC,
                CIT_DETAIL_SPEC,
                CIT_SUBTOTAL_SPEC
        );

        int stampHeaderRow = stampStyleRegistry.get(SummaryTemplateNamespace.STAMP_SECTION_HEADER).rowIndex();
        int commonHeaderRow = resolveRow(templateWorkbook, NR_COMMON_HEADER);
        int commonDetailRow = resolveRow(templateWorkbook, NR_COMMON_DETAIL);
        int commonSubtotalRow = resolveRow(templateWorkbook, NR_COMMON_SUBTOTAL);
        int finalTotalRow = resolveRow(templateWorkbook, NR_FINAL_TOTAL);

        int blockStart = stampHeaderRow;
        int blockEnd = finalTotalRow;
        summaryCells.deleteRows(blockStart, blockEnd - blockStart + 1, true);

        int cursor = blockStart;
        List<Integer> declaredSubtotalRows = new ArrayList<>();
        List<Integer> bookSubtotalRows = new ArrayList<>();

        SectionResult stampResult = renderStampSection(
                templateCells,
                summaryCells,
                cursor,
                summaryData.getStampDutyRows(),
                stampStyleRegistry
        );
        cursor = stampResult.getNextCursor();
        addIfNotNull(declaredSubtotalRows, stampResult.getDeclaredSubtotalRow());

        SectionResult vatResult = renderVatSection(
                outputWorkbook,
                templateCells,
                summaryCells,
                cursor,
                summaryData.getVatTaxRows(),
                stampStyleRegistry
        );
        cursor = vatResult.getNextCursor();
        addIfNotNull(declaredSubtotalRows, vatResult.getDeclaredSubtotalRow());
        addIfNotNull(bookSubtotalRows, vatResult.getBookSubtotalRow());

        SectionResult citResult = renderCitSection(
                templateCells,
                summaryCells,
                cursor,
                summaryData.getCorporateIncomeTaxRows(),
                stampStyleRegistry
        );
        cursor = citResult.getNextCursor();
        addIfNotNull(declaredSubtotalRows, citResult.getDeclaredSubtotalRow());

        SectionResult commonResult = renderCommonSectionWithoutSubtotal(
                templateCells,
                summaryCells,
                cursor,
                new SectionSpec(commonHeaderRow, commonDetailRow, commonSubtotalRow, true, true, true, false),
                summaryData.getCommonTaxRows(),
                this::fillCommonDetailRow
        );
        cursor = commonResult.getNextCursor();
        addRowsIfValid(declaredSubtotalRows, commonResult.getDetailStartRow(), commonResult.getDetailEndRow());
        addRowsIfValid(bookSubtotalRows, commonResult.getDetailStartRow(), commonResult.getDetailEndRow());

        insertRowCopy(summaryCells, templateCells, finalTotalRow, cursor);
        fillFinalTotal(summaryCells, cursor, summaryData.getFinalTotal(), declaredSubtotalRows, bookSubtotalRows);
        cursor++;
        // 某些区块是后续按模板行复制进来的，最后再做一次token替换，避免占位符残留。
        replaceGlobalTokens(summarySheet, summaryData);

        log.info("summary rendered: stampRows={}, commonRows={}, citRows={}, totalRows={}",
                size(summaryData.getStampDutyRows()),
                size(summaryData.getVatTaxRows()) + size(summaryData.getCommonTaxRows()),
                size(summaryData.getCorporateIncomeTaxRows()),
                cursor + 1);
    }

    private SectionResult renderStampSection(Cells templateCells,
                                             Cells summaryCells,
                                             int cursor,
                                             List<SummarySheetDTO.StampDutyItem> rows,
                                             SummaryTemplateStyleRegistry styleRegistry) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return new SectionResult(cursor, null, null);
        }

        List<SummarySheetDTO.StampDutyItem> detailRows = new ArrayList<>();
        SummarySheetDTO.StampDutyItem providedSubtotal = null;
        for (SummarySheetDTO.StampDutyItem row : rows) {
            if (row == null) {
                continue;
            }
            if (isStampSubtotalRow(row)) {
                if (providedSubtotal == null) {
                    providedSubtotal = row;
                }
                continue;
            }
            detailRows.add(row);
        }
        if (detailRows.isEmpty() && providedSubtotal == null) {
            return new SectionResult(cursor, null, null);
        }

        insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.STAMP_SECTION_HEADER, cursor++);
        int detailStart = cursor;
        int detailEnd = cursor - 1;

        for (SummarySheetDTO.StampDutyItem row : detailRows) {
            insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.STAMP_DETAIL_ROW, cursor);
            fillStampDetailRow(summaryCells, cursor, row);
            detailEnd = cursor;
            cursor++;
        }
        mergeSeqCellsByTaxTypeGroup(summaryCells, detailStart, detailEnd);
        mergeSameTextCells(summaryCells, detailStart, detailEnd, SummaryColumnMapping.COL_TAX_TYPE);
        mergeSameTextCells(summaryCells, detailStart, detailEnd, SummaryColumnMapping.COL_TAX_BASIS_DESC);

        insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.STAMP_SUBTOTAL_ROW, cursor);
        fillStampSubtotalRow(summaryCells, cursor, providedSubtotal, detailStart, detailEnd);
        log.info("summary stamp section rendered: detailCount={}, subtotalRow={}", detailRows.size(), cursor + 1);
        return new SectionResult(cursor + 1, cursor, null);
    }

    private SectionResult renderVatSection(Workbook outputWorkbook,
                                           Cells templateCells,
                                           Cells summaryCells,
                                           int cursor,
                                           List<SummarySheetDTO.CommonTaxItem> rows,
                                           SummaryTemplateStyleRegistry styleRegistry) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return new SectionResult(cursor, null, null);
        }

        List<SummarySheetDTO.CommonTaxItem> detailRows = new ArrayList<>();
        SummarySheetDTO.CommonTaxItem providedSubtotal = null;
        for (SummarySheetDTO.CommonTaxItem row : rows) {
            if (row == null) {
                continue;
            }
            if (isCommonSubtotalRow(row)) {
                if (providedSubtotal == null) {
                    providedSubtotal = row;
                }
                continue;
            }
            detailRows.add(row);
        }
        if (detailRows.isEmpty() && providedSubtotal == null) {
            return new SectionResult(cursor, null, null);
        }
        VatChangeTotalRefIndex vatRefIndex = buildVatChangeTotalRefIndex(outputWorkbook);
        insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.VAT_SECTION_HEADER, cursor++);
        int detailStart = cursor;
        int detailEnd = cursor - 1;

        for (SummarySheetDTO.CommonTaxItem row : detailRows) {
            insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.VAT_DETAIL_ROW, cursor);
            fillCommonDetailRow(summaryCells, cursor, row);
            applyVatManualBaseFormula(summaryCells, cursor, row, vatRefIndex);
            fillVatDetailVarianceFormula(summaryCells, cursor, row);
            detailEnd = cursor;
            cursor++;
        }
        mergeSeqCellsByTaxTypeGroup(summaryCells, detailStart, detailEnd);
        mergeSameTextCells(summaryCells, detailStart, detailEnd, SummaryColumnMapping.COL_TAX_TYPE);

        insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.VAT_SUBTOTAL_ROW, cursor);
        fillVatSubtotalRow(summaryCells, cursor, providedSubtotal, detailStart, detailEnd);
        log.info("summary vat section rendered: detailCount={}, subtotalRow={}", detailRows.size(), cursor + 1);
        return new SectionResult(cursor + 1, cursor, cursor);
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

        if (spec.isWithHeader()) {
            insertRowCopy(summaryCells, templateCells, spec.getHeaderTemplateRow(), cursor++);
        }
        int detailStart = cursor;
        for (T row : rows) {
            insertRowCopy(summaryCells, templateCells, spec.getDetailTemplateRow(), cursor);
            rowWriter.write(summaryCells, cursor, row);
            fillCommonDetailVarianceFormula(summaryCells, cursor);
            cursor++;
        }
        int detailEnd = cursor - 1;
        if (spec.isMergeTaxType()) {
            mergeSeqCellsByTaxTypeGroup(summaryCells, detailStart, detailEnd);
            mergeSameTextCells(summaryCells, detailStart, detailEnd, SummaryColumnMapping.COL_TAX_TYPE);
        }

        insertRowCopy(summaryCells, templateCells, spec.getSubtotalTemplateRow(), cursor);
        fillCommonSubtotalRow(summaryCells, cursor, detailStart, detailEnd, spec.isWithDeclaredSubtotal(), spec.isWithBookSubtotal());

        Integer declaredSubtotalRow = spec.isWithDeclaredSubtotal() ? cursor : null;
        Integer bookSubtotalRow = spec.isWithBookSubtotal() ? cursor : null;
        return new SectionResult(cursor + 1, declaredSubtotalRow, bookSubtotalRow);
    }

    private <T> SectionResult renderCommonSectionWithoutSubtotal(Cells templateCells,
                                                                 Cells summaryCells,
                                                                 int cursor,
                                                                 SectionSpec spec,
                                                                 List<T> rows,
                                                                 RowWriter<T> rowWriter) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return new SectionResult(cursor, null, null, null, null);
        }

        if (spec.isWithHeader()) {
            insertRowCopy(summaryCells, templateCells, spec.getHeaderTemplateRow(), cursor++);
        }
        int detailStart = cursor;
        for (T row : rows) {
            insertRowCopy(summaryCells, templateCells, spec.getDetailTemplateRow(), cursor);
            rowWriter.write(summaryCells, cursor, row);
            cursor++;
        }
        int detailEnd = cursor - 1;
        if (spec.isMergeTaxType()) {
            mergeSeqCellsByTaxTypeGroup(summaryCells, detailStart, detailEnd);
            mergeSameTextCells(summaryCells, detailStart, detailEnd, SummaryColumnMapping.COL_TAX_TYPE);
        }
        return new SectionResult(cursor, null, null, detailStart, detailEnd);
    }

    private SectionResult renderCitSection(Cells templateCells,
                                           Cells summaryCells,
                                           int cursor,
                                           List<SummarySheetDTO.CorporateIncomeTaxItem> rows,
                                           SummaryTemplateStyleRegistry styleRegistry) throws Exception {
        if (rows == null || rows.isEmpty()) {
            return new SectionResult(cursor, null, null);
        }

        List<SummarySheetDTO.CorporateIncomeTaxItem> detailRows = new ArrayList<>();
        SummarySheetDTO.CorporateIncomeTaxItem providedSubtotal = null;
        for (SummarySheetDTO.CorporateIncomeTaxItem row : rows) {
            if (row == null) {
                continue;
            }
            if (isCitSubtotalRow(row)) {
                if (providedSubtotal == null) {
                    providedSubtotal = row;
                }
                continue;
            }
            detailRows.add(row);
        }
        if (detailRows.isEmpty() && providedSubtotal == null) {
            return new SectionResult(cursor, null, null);
        }

        int sectionHeaderRow = cursor;
        insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.CIT_SECTION_HEADER, cursor++);
        int detailStart = cursor;
        int detailEnd = cursor - 1;
        for (SummarySheetDTO.CorporateIncomeTaxItem row : detailRows) {
            insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.CIT_DETAIL_ROW, cursor);
            fillCitDetailRow(summaryCells, cursor, row);
            fillCitRemainingLossFormula(summaryCells, cursor);
            detailEnd = cursor;
            cursor++;
        }
        mergeCitHeaderAndDetail(summaryCells, sectionHeaderRow, detailEnd);

        insertRowCopyByNamespace(summaryCells, templateCells, styleRegistry, SummaryTemplateNamespace.CIT_SUBTOTAL_ROW, cursor);
        if (providedSubtotal != null) {
            fillCitDetailRow(summaryCells, cursor, providedSubtotal);
            fillCitSubtotalIdentity(summaryCells, cursor, providedSubtotal);
        } else {
            // 无业务小计输入时，清掉模板占位符，避免出现在最终台账。
            clearCell(summaryCells, cursor, SummaryColumnMapping.COL_SEQ);
            clearCell(summaryCells, cursor, SummaryColumnMapping.COL_TAX_TYPE);
        }
        fillCitSubtotalRow(summaryCells, cursor, detailStart, detailEnd);
        log.info("summary cit section rendered: detailCount={}, subtotalRow={}", detailRows.size(), cursor + 1);
        return new SectionResult(cursor + 1, cursor, null);
    }

    private void fillStampDetailRow(Cells cells, int rowIndex, SummarySheetDTO.StampDutyItem row) {
        if (row == null) {
            return;
        }
        putSeq(cells, rowIndex, SummaryColumnMapping.COL_SEQ, row.getSeqNo());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_TYPE, row.getTaxType());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_ITEM, row.getTaxItem());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC, row.getTaxBasisDesc());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN, row.getTaxBaseQuarter());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO, row.getLevyRatio());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE, row.getTaxRate());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, row.getActualTaxPayable());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1, row.getTaxBaseMonth1());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT, row.getTaxBaseMonth2());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT, row.getTaxBaseMonth3());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON, row.getVarianceReason());
    }

    private void fillStampSubtotalRow(Cells cells,
                                      int rowIndex,
                                      SummarySheetDTO.StampDutyItem subtotal,
                                      int detailStart,
                                      int detailEnd) {
        if (subtotal != null) {
            fillStampDetailRow(cells, rowIndex, subtotal);
        }
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC);
        // 印花税小计行：按展示口径，清空非汇总字段，仅保留“实际应纳税额(D)”为汇总公式。
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON);
        if (detailStart <= detailEnd) {
            setSumFormula(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
        } else if (subtotal == null) {
            cells.get(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT).putValue(0);
        }
    }

    private void fillCommonSubtotalRow(Cells cells,
                                       int rowIndex,
                                       int detailStart,
                                       int detailEnd,
                                       boolean withDeclaredSubtotal,
                                       boolean withBookSubtotal) {
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC);
        if (withDeclaredSubtotal) {
            setSumFormula(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
        }
        if (withBookSubtotal) {
            setSumFormula(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT, detailStart, detailEnd);
        }
        if (withDeclaredSubtotal && withBookSubtotal) {
            String declaredCell = toCellRef(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT);
            String bookCell = toCellRef(rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT);
            cells.get(rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT).setFormula(declaredCell + "-" + bookCell);
        }
    }

    private void fillVatSubtotalRow(Cells cells,
                                    int rowIndex,
                                    SummarySheetDTO.CommonTaxItem subtotal,
                                    int detailStart,
                                    int detailEnd) {
        if (subtotal != null) {
            fillCommonDetailRow(cells, rowIndex, subtotal);
        }
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_ITEM);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC);
        // 增值税小计行：按展示口径，清空非汇总字段；D列优先使用字段值，不做强制公司合计。
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON);

        if (subtotal == null || subtotal.getActualTaxPayable() == null) {
            if (detailStart <= detailEnd) {
                setSumFormula(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
            } else {
                cells.get(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT).putValue(0);
            }
        }
    }

    private void fillVatDetailVarianceFormula(Cells cells, int rowIndex, SummarySheetDTO.CommonTaxItem row) {
        if (row == null || row.getActualTaxPayable() == null || row.getBookAmount() == null) {
            cells.get(rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT).putValue("");
            return;
        }
        String declaredCell = toCellRef(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT);
        String bookCell = toCellRef(rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT);
        cells.get(rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT).setFormula(declaredCell + "-" + bookCell);
    }

    private void fillCommonDetailVarianceFormula(Cells cells, int rowIndex) {
        String declaredCell = toCellRef(rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT);
        String bookCell = toCellRef(rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT);
        cells.get(rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT).setFormula(declaredCell + "-" + bookCell);
    }

    private void fillCommonDetailRow(Cells cells, int rowIndex, SummarySheetDTO.CommonTaxItem row) {
        if (row == null) {
            return;
        }
        putSeq(cells, rowIndex, SummaryColumnMapping.COL_SEQ, row.getSeqNo());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_TYPE, row.getTaxType());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_ITEM, row.getTaxItem());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC, row.getTaxBasisDesc());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN, row.getTaxBaseAmount());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO, row.getLevyRatio());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE, row.getTaxRate());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, row.getActualTaxPayable());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1, row.getAccountCode());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT, row.getBookAmount());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT, row.getVarianceAmount());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON, row.getVarianceReason());
    }

    private void applyVatManualBaseFormula(Cells cells,
                                           int rowIndex,
                                           SummarySheetDTO.CommonTaxItem row,
                                           VatChangeTotalRefIndex vatRefIndex) {
        if (row == null || vatRefIndex == null || vatRefIndex.isEmpty()) {
            return;
        }
        if (!normalizeText(row.getTaxType()).contains("增值税")) {
            return;
        }
        String taxItem = normalizeCompact(row.getTaxItem());
        String targetVatItem = null;
        if (normalizeCompact(VAT_ITEM_FIXED_ASSET_DISPOSAL).equals(taxItem)) {
            targetVatItem = VAT_ITEM_FIXED_ASSET_DISPOSAL;
        } else if (normalizeCompact(VAT_ITEM_FINANCE_INTEREST).equals(taxItem)) {
            targetVatItem = VAT_ITEM_FINANCE_INTEREST;
        } else if (normalizeCompact(VAT_ITEM_OTHER_INCOME).equals(taxItem)) {
            targetVatItem = VAT_ITEM_OTHER_INCOME;
        }
        if (targetVatItem == null) {
            return;
        }
        String formulaRef = vatRefIndex.getTotalCellRefByItem(targetVatItem);
        if (formulaRef == null || formulaRef.isBlank()) {
            return;
        }
        cells.get(rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN).setFormula("=" + formulaRef);
    }

    private void fillCitSubtotalRow(Cells cells, int rowIndex, int detailStart, int detailEnd) {
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC);
        setSumFormula(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, detailStart, detailEnd);
    }

    private void fillCitDetailRow(Cells cells, int rowIndex, SummarySheetDTO.CorporateIncomeTaxItem row) {
        if (row == null) {
            return;
        }
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_SEQ);
        clearCell(cells, rowIndex, SummaryColumnMapping.COL_TAX_TYPE);
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_ITEM, row.getProjectName());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASIS_DESC, row.getPreferentialPeriod());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_BASE_MAIN, row.getTaxableIncome());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_LEVY_RATIO, row.getTaxRate());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_TAX_RATE, row.getAnnualTaxPayable());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_DECLARED_AMOUNT, row.getQ1Tax());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_1, row.getQ2Tax());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_BOOK_AMOUNT, row.getQ3Tax());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_AMOUNT, row.getQ4Tax());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON, row.getQ1PayLastYearQ4());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_2, row.getLossCarryforwardUsed());
        putValueOrBlank(cells, rowIndex, SummaryColumnMapping.COL_EXTRA_3, row.getRemainingLossCarryforward());
    }

    private void fillCitRemainingLossFormula(Cells cells, int rowIndex) {
        String previousCarryRef = toCellRef(rowIndex, SummaryColumnMapping.COL_VARIANCE_REASON);
        String currentUsedRef = toCellRef(rowIndex, SummaryColumnMapping.COL_EXTRA_2);
        String formula = "IF(OR(" + previousCarryRef + "=\"\"," + currentUsedRef + "=\"\"),\"\"," 
                + previousCarryRef + "-" + currentUsedRef + ")";
        cells.get(rowIndex, SummaryColumnMapping.COL_EXTRA_3).setFormula(formula);
    }

    private void fillCitSubtotalIdentity(Cells cells,
                                         int rowIndex,
                                         SummarySheetDTO.CorporateIncomeTaxItem subtotal) {
        String seq = extractLeadingInteger(subtotal == null ? null : subtotal.getSeqNo());
        if (seq.isBlank()) {
            seq = extractLeadingInteger(subtotal == null ? null : subtotal.getProjectName());
        }
        if (seq.isBlank()) {
            seq = extractLeadingInteger(subtotal == null ? null : subtotal.getPreferentialPeriod());
        }
        cells.get(rowIndex, SummaryColumnMapping.COL_SEQ).putValue(seq);

        String taxType = firstNonBlank(
                subtotal == null ? null : subtotal.getProjectName(),
                subtotal == null ? null : subtotal.getPreferentialPeriod(),
                "企业所得税合计"
        );
        cells.get(rowIndex, SummaryColumnMapping.COL_TAX_TYPE).putValue(taxType);
    }

    private void fillFinalTotal(Cells cells,
                                int rowIndex,
                                SummarySheetDTO.FinalTotalItem totalItem,
                                List<Integer> declaredSubtotalRows,
                                List<Integer> bookSubtotalRows) {
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
        YearMonth periodYm = parseYearMonth(summaryData.getLedgerPeriod());
        String periodText = summaryData.getLedgerPeriod() == null ? "" : summaryData.getLedgerPeriod();
        String finalTotalTitle = resolveFinalTotalTitle(summaryData);
        replaceToken(summarySheet, "{{title}}", title);
        replacePeriodToken(summarySheet, "{{periodText}}", periodText, periodYm);
        replaceStampQuarterMonthLabels(summarySheet, periodYm);
        replaceCitQuarterLabels(summarySheet, periodYm);
        replaceToken(summarySheet, "{{finalTotalTitle}}", finalTotalTitle);
        replaceToken(summarySheet, "{{declaredTotal}}", "");
        replaceToken(summarySheet, "{{bookTotal}}", "");
    }

    private String resolveFinalTotalTitle(SummarySheetDTO summaryData) {
        if (summaryData == null || summaryData.getFinalTotal() == null) {
            return "合计";
        }
        String text = summaryData.getFinalTotal().getTotalTitle();
        return text == null || text.isBlank() ? "合计" : text.trim();
    }

    private void replaceCitQuarterLabels(Worksheet summarySheet, YearMonth periodYm) {
        if (periodYm == null) {
            replaceToken(summarySheet, "{{citQ1Label}}", "Q1应缴税额");
            replaceToken(summarySheet, "{{citQ2Label}}", "Q2应缴税额");
            replaceToken(summarySheet, "{{citQ3Label}}", "Q3应缴税额");
            replaceToken(summarySheet, "{{citQ4Label}}", "Q4应缴税额");
            return;
        }
        int month = periodYm.getMonthValue();
        int currentQuarter = ((month - 1) / 3) + 1;
        String[] labels = new String[4];
        for (int i = 0; i < 4; i++) {
            int quarter = ((currentQuarter - 1 + i) % 4) + 1;
            boolean ended = month >= quarter * 3;
            labels[i] = "Q" + quarter + (ended ? "已缴税额" : "应缴税额");
        }
        replaceToken(summarySheet, "{{citQ1Label}}", labels[0]);
        replaceToken(summarySheet, "{{citQ2Label}}", labels[1]);
        replaceToken(summarySheet, "{{citQ3Label}}", labels[2]);
        replaceToken(summarySheet, "{{citQ4Label}}", labels[3]);
    }

    private void replaceStampQuarterMonthLabels(Worksheet summarySheet, YearMonth periodYm) {
        if (periodYm == null) {
            replaceToken(summarySheet, "{{stampMonth1Label}}", "计税金额-");
            replaceToken(summarySheet, "{{stampMonth2Label}}", "计税金额-");
            replaceToken(summarySheet, "{{stampMonth3Label}}", "计税金额-");
            return;
        }
        int month = periodYm.getMonthValue();
        int quarterStartMonth = ((month - 1) / 3) * 3 + 1;
        YearMonth m1 = YearMonth.of(periodYm.getYear(), quarterStartMonth);
        YearMonth m2 = m1.plusMonths(1);
        YearMonth m3 = m1.plusMonths(2);
        replaceToken(summarySheet, "{{stampMonth1Label}}", buildStampMonthLabel(m1));
        replaceToken(summarySheet, "{{stampMonth2Label}}", buildStampMonthLabel(m2));
        replaceToken(summarySheet, "{{stampMonth3Label}}", buildStampMonthLabel(m3));
    }

    private String buildStampMonthLabel(YearMonth ym) {
        String mm = String.format("%02d", ym.getMonthValue());
        return "计税金额-" + ym.getYear() + "年" + mm + "月";
    }

    private void replacePeriodToken(Worksheet sheet, String token, String fallbackText, YearMonth periodYm) {
        Cells cells = sheet.getCells();
        int maxRow = cells.getMaxRow();
        int maxCol = cells.getMaxColumn();
        for (int row = 0; row <= maxRow; row++) {
            for (int col = 0; col <= maxCol; col++) {
                String value = cells.get(row, col).getStringValue();
                if (value == null || value.isEmpty() || !value.contains(token)) {
                    continue;
                }
                if (token.equals(value.trim()) && periodYm != null) {
                    LocalDate firstDay = periodYm.atDay(1);
                    cells.get(row, col).putValue(Date.from(firstDay.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
                    continue;
                }
                String replacement = periodYm == null
                        ? fallbackText
                        : periodYm.getYear() + "/" + periodYm.getMonthValue() + "/1";
                cells.get(row, col).putValue(value.replace(token, replacement));
            }
        }
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

    private void insertRowCopyByNamespace(Cells targetCells,
                                          Cells sourceCells,
                                          SummaryTemplateStyleRegistry styleRegistry,
                                          SummaryTemplateNamespace namespace,
                                          int destinationRowIndex) throws Exception {
        SummaryTemplateStyleRegistry.ResolvedTemplateRow resolved = styleRegistry.get(namespace);
        targetCells.insertRows(destinationRowIndex, 1);
        targetCells.copyRow(sourceCells, resolved.rowIndex(), destinationRowIndex);
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

    private String toCellRef(int rowIndex, int colIndex) {
        return toColumnName(colIndex) + (rowIndex + 1);
    }

    private void putValueOrBlank(Cells cells, int row, int col, Object value) {
        if (value == null) {
            clearCell(cells, row, col);
            return;
        }
        if (value instanceof BigDecimal) {
            BigDecimal bigDecimal = (BigDecimal) value;
            cells.get(row, col).putValue(bigDecimal.doubleValue());
            return;
        }
        cells.get(row, col).putValue(value);
    }

    private void putSeq(Cells cells, int row, int col, Object value) {
        if (value == null) {
            clearCell(cells, row, col);
            return;
        }
        String text = String.valueOf(value).trim();
        cells.get(row, col).putValue(text);
    }

    private void clearCell(Cells cells, int row, int col) {
        cells.get(row, col).putValue("");
    }

    private String normalizeCompact(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\u00A0", "")
                .replace(" ", "")
                .replace("（", "(")
                .replace("）", ")")
                .trim();
    }

    private String escapeSheetName(String sheetName) {
        if (sheetName == null) {
            return "";
        }
        return sheetName.replace("'", "''");
    }

    private VatChangeTotalRefIndex buildVatChangeTotalRefIndex(Workbook workbook) {
        if (workbook == null) {
            return VatChangeTotalRefIndex.createEmpty();
        }
        Worksheet vatSheet = workbook.getWorksheets().get(VAT_CHANGE_SHEET_NAME);
        if (vatSheet == null) {
            return VatChangeTotalRefIndex.createEmpty();
        }
        Cells cells = vatSheet.getCells();
        HeaderLocateResult header = locateVatHeader(cells);
        if (header == null) {
            return VatChangeTotalRefIndex.createEmpty();
        }
        String escapedSheetName = escapeSheetName(vatSheet.getName());
        VatChangeTotalRefIndex index = new VatChangeTotalRefIndex();
        int maxRow = cells.getMaxDataRow();
        for (int row = header.headerRow + 1; row <= maxRow; row++) {
            String item = normalizeCompact(cells.get(row, header.itemCol).getStringValue());
            if (item.isBlank()) {
                continue;
            }
            String ref = "'" + escapedSheetName + "'!" + toColumnName(header.totalCol) + (row + 1);
            index.put(item, ref);
        }
        return index;
    }

    private HeaderLocateResult locateVatHeader(Cells cells) {
        int maxRow = Math.max(0, cells.getMaxDataRow());
        int maxCol = Math.max(0, cells.getMaxDataColumn());
        int scanRows = Math.min(maxRow, 30);
        for (int row = 0; row <= scanRows; row++) {
            Integer itemCol = null;
            Integer totalCol = null;
            for (int col = 0; col <= maxCol; col++) {
                String v = normalizeCompact(cells.get(row, col).getStringValue());
                if (VAT_ITEM_HEADER.equals(v)) {
                    itemCol = col;
                } else if (VAT_TOTAL_HEADER.equals(v)) {
                    totalCol = col;
                }
            }
            if (itemCol != null && totalCol != null) {
                return new HeaderLocateResult(row, itemCol, totalCol);
            }
        }
        return null;
    }


    private boolean isStampSubtotalRow(SummarySheetDTO.StampDutyItem row) {
        if (row == null) {
            return false;
        }
        String taxType = normalizeText(row.getTaxType());
        String taxItem = normalizeText(row.getTaxItem());
        return taxType.contains("合计") || taxItem.contains("合计");
    }

    private String normalizeText(String text) {
        return text == null ? "" : text.replace("\u00A0", " ").trim().toLowerCase(Locale.ROOT);
    }

    private String extractLeadingInteger(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        Matcher matcher = LEADING_INTEGER.matcher(text);
        if (!matcher.matches()) {
            return "";
        }
        return matcher.group(1);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private void mergeSameTextCells(Cells cells, int startRow, int endRow, int colIndex) {
        if (startRow >= endRow) {
            return;
        }
        int groupStart = startRow;
        String current = normalizeText(cells.get(startRow, colIndex).getStringValue());
        for (int row = startRow + 1; row <= endRow + 1; row++) {
            String text = row <= endRow ? normalizeText(cells.get(row, colIndex).getStringValue()) : null;
            boolean same = row <= endRow && text.equals(current);
            if (same) {
                continue;
            }
            int count = row - groupStart;
            if (count > 1 && current != null && !current.isBlank()) {
                cells.merge(groupStart, colIndex, count, 1);
            }
            groupStart = row;
            current = text;
        }
    }

    private void mergeSeqCellsByTaxTypeGroup(Cells cells, int startRow, int endRow) {
        if (startRow >= endRow) {
            return;
        }
        int groupStart = startRow;
        String current = normalizeText(cells.get(startRow, SummaryColumnMapping.COL_TAX_TYPE).getStringValue());
        for (int row = startRow + 1; row <= endRow + 1; row++) {
            String text = row <= endRow ? normalizeText(cells.get(row, SummaryColumnMapping.COL_TAX_TYPE).getStringValue()) : null;
            boolean same = row <= endRow && text.equals(current);
            if (same) {
                continue;
            }
            int count = row - groupStart;
            if (count > 1 && current != null && !current.isBlank()) {
                cells.merge(groupStart, SummaryColumnMapping.COL_SEQ, count, 1);
            }
            groupStart = row;
            current = text;
        }
    }

    private void mergeCitHeaderAndDetail(Cells cells, int headerRow, int detailEndRow) {
        if (detailEndRow <= headerRow) {
            return;
        }
        int count = detailEndRow - headerRow + 1;
        cells.merge(headerRow, SummaryColumnMapping.COL_SEQ, count, 1);
        cells.merge(headerRow, SummaryColumnMapping.COL_TAX_TYPE, count, 1);
    }

    private boolean isCommonSubtotalRow(SummarySheetDTO.CommonTaxItem row) {
        if (row == null) {
            return false;
        }
        String taxType = normalizeText(row.getTaxType());
        String taxItem = normalizeText(row.getTaxItem());
        return taxType.contains("合计") || taxItem.contains("合计");
    }

    private boolean isCitSubtotalRow(SummarySheetDTO.CorporateIncomeTaxItem row) {
        if (row == null) {
            return false;
        }
        String projectName = normalizeText(row.getProjectName());
        String period = normalizeText(row.getPreferentialPeriod());
        return projectName.contains("合计") || period.contains("合计");
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

    private void addRowsIfValid(List<Integer> rows, Integer startRow, Integer endRow) {
        if (startRow == null || endRow == null || startRow > endRow) {
            return;
        }
        for (int row = startRow; row <= endRow; row++) {
            rows.add(row);
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
        private final boolean mergeTaxType;
        private final boolean withHeader;

        private SectionSpec(int headerTemplateRow,
                            int detailTemplateRow,
                            int subtotalTemplateRow,
                            boolean withDeclaredSubtotal,
                            boolean withBookSubtotal) {
            this(headerTemplateRow, detailTemplateRow, subtotalTemplateRow, withDeclaredSubtotal, withBookSubtotal, false, true);
        }

        private SectionSpec(int headerTemplateRow,
                            int detailTemplateRow,
                            int subtotalTemplateRow,
                            boolean withDeclaredSubtotal,
                            boolean withBookSubtotal,
                            boolean mergeTaxType) {
            this(headerTemplateRow, detailTemplateRow, subtotalTemplateRow, withDeclaredSubtotal, withBookSubtotal, mergeTaxType, true);
        }

        private SectionSpec(int headerTemplateRow,
                            int detailTemplateRow,
                            int subtotalTemplateRow,
                            boolean withDeclaredSubtotal,
                            boolean withBookSubtotal,
                            boolean mergeTaxType,
                            boolean withHeader) {
            this.headerTemplateRow = headerTemplateRow;
            this.detailTemplateRow = detailTemplateRow;
            this.subtotalTemplateRow = subtotalTemplateRow;
            this.withDeclaredSubtotal = withDeclaredSubtotal;
            this.withBookSubtotal = withBookSubtotal;
            this.mergeTaxType = mergeTaxType;
            this.withHeader = withHeader;
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

        private boolean isMergeTaxType() {
            return mergeTaxType;
        }

        private boolean isWithHeader() {
            return withHeader;
        }
    }

    private static class SectionResult {
        private final int nextCursor;
        private final Integer declaredSubtotalRow;
        private final Integer bookSubtotalRow;
        private final Integer detailStartRow;
        private final Integer detailEndRow;

        private SectionResult(int nextCursor, Integer declaredSubtotalRow, Integer bookSubtotalRow) {
            this(nextCursor, declaredSubtotalRow, bookSubtotalRow, null, null);
        }

        private SectionResult(int nextCursor,
                              Integer declaredSubtotalRow,
                              Integer bookSubtotalRow,
                              Integer detailStartRow,
                              Integer detailEndRow) {
            this.nextCursor = nextCursor;
            this.declaredSubtotalRow = declaredSubtotalRow;
            this.bookSubtotalRow = bookSubtotalRow;
            this.detailStartRow = detailStartRow;
            this.detailEndRow = detailEndRow;
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

        private Integer getDetailStartRow() {
            return detailStartRow;
        }

        private Integer getDetailEndRow() {
            return detailEndRow;
        }
    }

    private static class HeaderLocateResult {
        private final int headerRow;
        private final int itemCol;
        private final int totalCol;

        private HeaderLocateResult(int headerRow, int itemCol, int totalCol) {
            this.headerRow = headerRow;
            this.itemCol = itemCol;
            this.totalCol = totalCol;
        }
    }

    private static class VatChangeTotalRefIndex {
        private final Map<String, String> totalRefByItem = new LinkedHashMap<>();

        private static VatChangeTotalRefIndex createEmpty() {
            return new VatChangeTotalRefIndex();
        }

        private void put(String itemName, String cellRef) {
            if (itemName == null || itemName.isBlank() || cellRef == null || cellRef.isBlank()) {
                return;
            }
            totalRefByItem.putIfAbsent(compact(itemName), cellRef);
        }

        private String getTotalCellRefByItem(String itemName) {
            return totalRefByItem.get(compact(itemName));
        }

        private boolean isEmpty() {
            return totalRefByItem.isEmpty();
        }

        private String compact(String text) {
            if (text == null) {
                return "";
            }
            return text.replace("\u00A0", "")
                    .replace(" ", "")
                    .replace("（", "(")
                    .replace("）", ")")
                    .trim();
        }
    }

}
