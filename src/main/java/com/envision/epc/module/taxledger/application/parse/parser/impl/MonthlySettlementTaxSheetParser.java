package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.envision.epc.module.taxledger.application.dto.MonthlyTaxSectionDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 睿景景程月结数据表-报税解析器。
 * 对应sheet页：睿景景程月结数据表-报税
 * 对应类别：FileCategoryEnum.MONTHLY_SETTLEMENT_TAX
 */
@Component
public class MonthlySettlementTaxSheetParser implements SheetParser<List<MonthlyTaxSectionDTO>> {
    private static final String HEADER_COST = "成本";
    private static final String HEADER_INCOME = "收入";
    private static final String HEADER_OUTPUT_TAX = "销项";
    private static final String HEADER_INVOICED_INCOME = "已开票收入";
    private static final String HEADER_INVOICED_TAX_AMOUNT = "已开票税额";
    private static final String HEADER_UNINVOICED_INCOME = "未开票收入";
    private static final String HEADER_UNINVOICED_TAX_AMOUNT = "未开票税额";
    private static final Pattern TAX_RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]");

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.MONTHLY_SETTLEMENT_TAX;
    }

    @Override
    public Class<List<MonthlyTaxSectionDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<MonthlyTaxSectionDTO>> cls = (Class<List<MonthlyTaxSectionDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<MonthlyTaxSectionDTO>> parse(InputStream inputStream, ParseContext context) {
        List<MonthlyTaxSectionDTO> sections = new ArrayList<>();
        ParseResult<List<MonthlyTaxSectionDTO>> result = ParseResult.<List<MonthlyTaxSectionDTO>>builder()
                .data(sections)
                .build();
        if (inputStream == null) {
            result.addIssue("睿景景程月结数据表-报税：文件流为空");
            return result;
        }

        List<Map<Integer, String>> rows = readRows(inputStream, result);
        if (rows.isEmpty()) {
            result.addIssue("睿景景程月结数据表-报税：无可解析数据");
            return result;
        }

        int headerRowIndex = findHeaderRow(rows);
        if (headerRowIndex < 0) {
            result.addIssue("睿景景程月结数据表-报税：未识别到表头（成本/收入/销项等）");
            return result;
        }
        if (headerRowIndex + 1 >= rows.size()) {
            result.addIssue("睿景景程月结数据表-报税：缺少数据行");
            return result;
        }

        Map<Integer, String> titleRow = headerRowIndex > 0 ? rows.get(headerRowIndex - 1) : Map.of();
        Map<Integer, String> headerRow = rows.get(headerRowIndex);
        int dataStartRowIndex = headerRowIndex + 1;

        List<Integer> sectionStartCols = findSectionStartColumns(headerRow);
        if (sectionStartCols.isEmpty()) {
            result.addIssue("睿景景程月结数据表-报税：未识别到任何分段起始列");
            return result;
        }

        for (Integer startCol : sectionStartCols) {
            String title = findSectionTitle(titleRow, startCol);
            String taxRate = extractTaxRate(title);
            parseSectionRows(rows, dataStartRowIndex, startCol, title, taxRate, sections);
        }
        return result;
    }

    private static void parseSectionRows(List<Map<Integer, String>> rows,
                                         int dataStartRowIndex,
                                         int startCol,
                                         String title,
                                         String taxRate,
                                         List<MonthlyTaxSectionDTO> sections) {
        boolean started = false;
        for (int rowIndex = dataStartRowIndex; rowIndex < rows.size(); rowIndex++) {
            Map<Integer, String> row = rows.get(rowIndex);
            String costRaw = normalize(row.get(startCol));
            String incomeRaw = normalize(row.get(startCol + 1));
            String outputTaxRaw = normalize(row.get(startCol + 2));
            String invoicedIncomeRaw = normalize(row.get(startCol + 3));
            String invoicedTaxAmountRaw = normalize(row.get(startCol + 4));
            String uninvoicedIncomeRaw = normalize(row.get(startCol + 5));
            String uninvoicedTaxAmountRaw = normalize(row.get(startCol + 6));

            if (allBlank(costRaw, incomeRaw, outputTaxRaw,
                    invoicedIncomeRaw, invoicedTaxAmountRaw, uninvoicedIncomeRaw, uninvoicedTaxAmountRaw)) {
                if (started) {
                    break;
                }
                continue;
            }
            started = true;

            MonthlyTaxSectionDTO section = new MonthlyTaxSectionDTO();
            section.setTitle(title);
            section.setTaxRate(taxRate);
            section.setCost(ParserValueUtils.toBigDecimal(costRaw));
            section.setIncome(ParserValueUtils.toBigDecimal(incomeRaw));
            section.setOutputTax(ParserValueUtils.toBigDecimal(outputTaxRaw));
            section.setInvoicedIncome(ParserValueUtils.toBigDecimal(invoicedIncomeRaw));
            section.setInvoicedTaxAmount(ParserValueUtils.toBigDecimal(invoicedTaxAmountRaw));
            section.setUninvoicedIncome(ParserValueUtils.toBigDecimal(uninvoicedIncomeRaw));
            section.setUninvoicedTaxAmount(ParserValueUtils.toBigDecimal(uninvoicedTaxAmountRaw));
            sections.add(section);
        }
    }

    private static List<Map<Integer, String>> readRows(InputStream inputStream,
                                                       ParseResult<List<MonthlyTaxSectionDTO>> result) {
        List<Map<Integer, String>> rows = new ArrayList<>();
        try {
            EasyExcelFactory.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
                        @Override
                        public void invoke(Map<Integer, String> rowData, AnalysisContext analysisContext) {
                            rows.add(new HashMap<>(rowData));
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                            // no-op
                        }
                    })
                    .headRowNumber(0)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            result.addIssue("睿景景程月结数据表-报税：读取Excel失败 - " + e.getMessage());
            return List.of();
        }
        return rows;
    }

    private static int findHeaderRow(List<Map<Integer, String>> rows) {
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            if (containsHeader(row, HEADER_COST)
                    && containsHeader(row, HEADER_INCOME)
                    && containsHeader(row, HEADER_OUTPUT_TAX)
                    && containsHeader(row, HEADER_INVOICED_INCOME)
                    && containsHeader(row, HEADER_INVOICED_TAX_AMOUNT)
                    && containsHeader(row, HEADER_UNINVOICED_INCOME)
                    && containsHeader(row, HEADER_UNINVOICED_TAX_AMOUNT)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean containsHeader(Map<Integer, String> row, String expected) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        String normalizedExpected = normalize(expected);
        for (String value : row.values()) {
            if (normalizedExpected.equals(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private static List<Integer> findSectionStartColumns(Map<Integer, String> headerRow) {
        List<Integer> starts = new ArrayList<>();
        if (headerRow == null || headerRow.isEmpty()) {
            return starts;
        }
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            String value = normalize(entry.getValue());
            if (HEADER_COST.equals(value) && isValidSectionHeaderBlock(headerRow, entry.getKey())) {
                starts.add(entry.getKey());
            }
        }
        starts.sort(Integer::compareTo);
        return starts;
    }

    private static boolean isValidSectionHeaderBlock(Map<Integer, String> headerRow, int startCol) {
        return HEADER_COST.equals(normalize(headerRow.get(startCol)))
                && HEADER_INCOME.equals(normalize(headerRow.get(startCol + 1)))
                && HEADER_OUTPUT_TAX.equals(normalize(headerRow.get(startCol + 2)))
                && HEADER_INVOICED_INCOME.equals(normalize(headerRow.get(startCol + 3)))
                && HEADER_INVOICED_TAX_AMOUNT.equals(normalize(headerRow.get(startCol + 4)))
                && HEADER_UNINVOICED_INCOME.equals(normalize(headerRow.get(startCol + 5)))
                && HEADER_UNINVOICED_TAX_AMOUNT.equals(normalize(headerRow.get(startCol + 6)));
    }

    private static String findSectionTitle(Map<Integer, String> titleRow, int startCol) {
        if (titleRow == null || titleRow.isEmpty()) {
            return null;
        }
        for (int col = startCol; col <= startCol + 6; col++) {
            String title = normalize(titleRow.get(col));
            if (StringUtils.hasText(title)) {
                return title;
            }
        }
        return null;
    }

    private static String extractTaxRate(String title) {
        if (!StringUtils.hasText(title)) {
            return null;
        }
        Matcher matcher = TAX_RATE_PATTERN.matcher(title);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "%";
    }

    private static boolean allBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return false;
            }
        }
        return true;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\u00A0", " ").trim();
    }
}
