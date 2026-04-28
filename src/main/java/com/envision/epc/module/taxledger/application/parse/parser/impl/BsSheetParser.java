package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.BsStatementRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.SheetSelectUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * BS（资产负债表）解析器。
 * 对应sheet页：资产负债表
 * 对应类别：FileCategoryEnum.BS
 */
@Component
public class BsSheetParser implements SheetParser<List<BsStatementRowDTO>> {
    private static final int LEFT_BASE_COL = 0;   // A-D
    private static final int RIGHT_BASE_COL = 4;  // E-H

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.BS;
    }

    @Override
    public Class<List<BsStatementRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<BsStatementRowDTO>> cls = (Class<List<BsStatementRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<BsStatementRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<BsStatementRowDTO>> result = ParseResult.<List<BsStatementRowDTO>>builder()
                .data(List.of())
                .build();
        try {
            Workbook workbook = new Workbook(inputStream);
            Worksheet sheet = SheetSelectUtils.resolveAsposeSheet(workbook, category());
            Cells cells = sheet.getCells();
            int maxRow = cells.getMaxDataRow();
            Map<Integer, BsStatementRowDTO> rowMap = new TreeMap<>();

            for (int rowIdx = 0; rowIdx <= maxRow; rowIdx++) {
                mergeRow(rowMap, parseHalfRow(cells, rowIdx, LEFT_BASE_COL));
                mergeRow(rowMap, parseHalfRow(cells, rowIdx, RIGHT_BASE_COL));
            }

            if (rowMap.isEmpty()) {
                result.addIssue("INVALID_WORKBOOK: no BS line records found");
                return result;
            }

            List<BsStatementRowDTO> rows = new ArrayList<>(rowMap.size());
            for (Map.Entry<Integer, BsStatementRowDTO> entry : rowMap.entrySet()) {
                BsStatementRowDTO existing = entry.getValue();
                existing.setLineNo(String.valueOf(entry.getKey()));
                rows.add(existing);
            }
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private BsStatementRowDTO parseHalfRow(Cells cells, int rowIdx, int baseCol) {
        String lineNoText = normalize(getCellText(cells, rowIdx, baseCol + 1));
        if (!isDigits(lineNoText)) {
            return null;
        }
        BsStatementRowDTO row = new BsStatementRowDTO();
        row.setLineNo(lineNoText);
        row.setItemName(normalize(getCellText(cells, rowIdx, baseCol)));
        row.setYearStartAmount(parseAmount(cells, rowIdx, baseCol + 2));
        row.setAccumulatedAmount(parseAmount(cells, rowIdx, baseCol + 3));
        return row;
    }

    private void mergeRow(Map<Integer, BsStatementRowDTO> rowMap, BsStatementRowDTO incoming) {
        if (incoming == null || !isDigits(incoming.getLineNo())) {
            return;
        }
        int lineNo = Integer.parseInt(incoming.getLineNo());
        BsStatementRowDTO existing = rowMap.get(lineNo);
        if (existing == null) {
            rowMap.put(lineNo, incoming);
            return;
        }
        if (isBlank(existing.getItemName()) && !isBlank(incoming.getItemName())) {
            existing.setItemName(incoming.getItemName());
        }
        if (existing.getYearStartAmount() == null && incoming.getYearStartAmount() != null) {
            existing.setYearStartAmount(incoming.getYearStartAmount());
        }
        if (existing.getAccumulatedAmount() == null && incoming.getAccumulatedAmount() != null) {
            existing.setAccumulatedAmount(incoming.getAccumulatedAmount());
        }
    }

    private String getCellText(Cells cells, int row, int col) {
        Cell cell = cells.get(row, col);
        if (cell == null || cell.getValue() == null) {
            return "";
        }
        return cell.getStringValue();
    }

    private BigDecimal parseAmount(Cells cells, int row, int col) {
        String text = normalize(getCellText(cells, row, col));
        if (isBlank(text) || "-".equals(text)) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(text.replace(",", ""));
        } catch (Exception ignored) {
            return BigDecimal.ZERO;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u00A0", " ").trim();
    }

    private boolean isDigits(String value) {
        if (isBlank(value)) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
