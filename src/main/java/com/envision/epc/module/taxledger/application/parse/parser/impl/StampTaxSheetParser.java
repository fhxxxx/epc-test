package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.SheetSelectUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 印花税明细解析器。
 * 对应sheet页：印花税明细-2320、2355
 * 对应类别：FileCategoryEnum.STAMP_TAX
 */
@Component
public class StampTaxSheetParser implements SheetParser<List<StampDutySummaryRowDTO>> {
    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final String SUMMARY_MARK = "汇总";
    private static final String COL_CONTRACT_CATEGORY = "合同类别";
    private static final String COL_TAXABLE_AMOUNT = "应税金额";
    private static final String COL_TAX_RATE = "税率";
    private static final String COL_TAX_PAYABLE_AMOUNT = "应纳税额";

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.STAMP_TAX;
    }

    @Override
    public Class<List<StampDutySummaryRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<StampDutySummaryRowDTO>> cls = (Class<List<StampDutySummaryRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<StampDutySummaryRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<StampDutySummaryRowDTO>> result = ParseResult.<List<StampDutySummaryRowDTO>>builder()
                .data(List.of())
                .build();
        try {
            List<Map<Integer, String>> rawRows = new ArrayList<>();
            byte[] bytes = inputStream.readAllBytes();
            String sheetName = SheetSelectUtils.resolveEasyExcelSheetName(bytes, category());
            EasyExcelFactory.read(new ByteArrayInputStream(bytes), new AnalysisEventListener<Map<Integer, String>>() {
                        @Override
                        public void invoke(Map<Integer, String> data, AnalysisContext context) {
                            rawRows.add(new HashMap<>(data));
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            // no-op
                        }
                    })
                    .headRowNumber(0)
                    .sheet(sheetName)
                    .doRead();

            List<StampDutySummaryRowDTO> rows = parseSummaryBlock(rawRows);
            if (rows.isEmpty()) {
                result.addIssue("INVALID_WORKBOOK: no stamp duty summary rows found");
                return result;
            }
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private List<StampDutySummaryRowDTO> parseSummaryBlock(List<Map<Integer, String>> rawRows) {
        List<StampDutySummaryRowDTO> rows = new ArrayList<>();
        int headerRowIdx = findSummaryHeaderRow(rawRows);
        if (headerRowIdx < 0) {
            return rows;
        }
        Map<String, Integer> colIndex = buildHeaderIndex(rawRows.get(headerRowIdx));
        Integer contractCol = colIndex.get(COL_CONTRACT_CATEGORY);
        Integer taxableCol = colIndex.get(COL_TAXABLE_AMOUNT);
        Integer taxRateCol = colIndex.get(COL_TAX_RATE);
        Integer taxPayableCol = colIndex.get(COL_TAX_PAYABLE_AMOUNT);
        if (contractCol == null || taxableCol == null || taxRateCol == null || taxPayableCol == null) {
            return rows;
        }

        for (int dataRowIdx = headerRowIdx + 1; dataRowIdx < rawRows.size(); dataRowIdx++) {
            Map<Integer, String> dataRow = rawRows.get(dataRowIdx);
            if (isBlankRow(dataRow) || isHeaderRow(dataRow)) {
                break;
            }

            String contractCategory = normalize(get(dataRow, contractCol));
            BigDecimal taxableAmount = defaultZero(ParserValueUtils.toBigDecimal(get(dataRow, taxableCol)));
            BigDecimal taxRate = defaultZero(ParserValueUtils.toBigDecimal(get(dataRow, taxRateCol)));
            BigDecimal taxPayableAmount = defaultZero(ParserValueUtils.toBigDecimal(get(dataRow, taxPayableCol)));

            if (!StringUtils.hasText(contractCategory)
                    && isZero(taxableAmount)
                    && isZero(taxRate)
                    && isZero(taxPayableAmount)) {
                continue;
            }

            StampDutySummaryRowDTO dto = new StampDutySummaryRowDTO();
            dto.setContractCategory(contractCategory);
            dto.setTaxableAmount(taxableAmount);
            dto.setTaxRate(taxRate);
            dto.setTaxPayableAmount(taxPayableAmount);
            rows.add(dto);
        }
        return rows;
    }

    private int findSummaryHeaderRow(List<Map<Integer, String>> rawRows) {
        for (int rowIdx = 0; rowIdx < rawRows.size(); rowIdx++) {
            Map<Integer, String> row = rawRows.get(rowIdx);
            if (!isHeaderRow(row)) {
                continue;
            }
            boolean hasSummaryMark = containsValue(row, SUMMARY_MARK)
                    || (rowIdx > 0 && containsValue(rawRows.get(rowIdx - 1), SUMMARY_MARK));
            if (hasSummaryMark) {
                return rowIdx;
            }
        }
        return -1;
    }

    private boolean isHeaderRow(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        boolean hasContract = false;
        boolean hasTaxable = false;
        boolean hasRate = false;
        boolean hasPayable = false;
        for (String value : row.values()) {
            String normalized = normalize(value);
            if (COL_CONTRACT_CATEGORY.equals(normalized)) {
                hasContract = true;
            } else if (COL_TAXABLE_AMOUNT.equals(normalized)) {
                hasTaxable = true;
            } else if (COL_TAX_RATE.equals(normalized)) {
                hasRate = true;
            } else if (COL_TAX_PAYABLE_AMOUNT.equals(normalized)) {
                hasPayable = true;
            }
        }
        return hasContract && hasTaxable && hasRate && hasPayable;
    }

    private Map<String, Integer> buildHeaderIndex(Map<Integer, String> headerRow) {
        Map<String, Integer> indexMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            String normalized = normalize(entry.getValue());
            if (StringUtils.hasText(normalized)) {
                indexMap.putIfAbsent(normalized, entry.getKey());
            }
        }
        return indexMap;
    }

    private boolean isBlankRow(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String value : row.values()) {
            if (StringUtils.hasText(normalize(value))) {
                return false;
            }
        }
        return true;
    }

    private boolean containsValue(Map<Integer, String> row, String expected) {
        if (row == null || row.isEmpty()) {
            return false;
        }
        for (String value : row.values()) {
            String normalized = normalize(value);
            if (normalized.contains(expected)) {
                return true;
            }
        }
        return false;
    }

    private String get(Map<Integer, String> row, Integer colIdx) {
        if (row == null || colIdx == null) {
            return "";
        }
        return row.getOrDefault(colIdx, "");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u00A0", " ").trim();
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private boolean isZero(BigDecimal value) {
        return value == null || value.compareTo(ZERO) == 0;
    }
}
