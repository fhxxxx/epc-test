package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * PL（利润表）解析器。
 * 对应sheet页：利润表
 * 对应类别：FileCategoryEnum.PL
 */
@Component
public class PlSheetParser implements SheetParser<List<PlStatementRowDTO>> {
    private static final int ITEM_NAME_COL = 0;          // A列：项目
    private static final int LINE_NO_COL = 1;            // B列：行号
    private static final int CURRENT_PERIOD_COL = 2;     // C列：本期发生额
    private static final int ACCUMULATED_COL = 3;        // D列：累计发生额

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.PL;
    }

    @Override
    public Class<List<PlStatementRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<PlStatementRowDTO>> cls = (Class<List<PlStatementRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<PlStatementRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<PlStatementRowDTO>> result = ParseResult.<List<PlStatementRowDTO>>builder()
                .data(List.of())
                .build();
        try {
            Workbook workbook = new Workbook(inputStream);
            Worksheet sheet = workbook.getWorksheets().get(0);
            Cells cells = sheet.getCells();
            int maxRow = cells.getMaxDataRow();

            List<PlStatementRowDTO> rows = new ArrayList<>();
            for (int rowIdx = 0; rowIdx <= maxRow; rowIdx++) {
                String lineNo = normalize(getCellText(cells, rowIdx, LINE_NO_COL));
                if (!isDigits(lineNo)) {
                    continue;
                }
                PlStatementRowDTO row = new PlStatementRowDTO();
                row.setLineNo(lineNo);
                row.setItemName(normalize(getCellText(cells, rowIdx, ITEM_NAME_COL)));
                row.setCurrentPeriodAmount(ParserValueUtils.toBigDecimal(getCellText(cells, rowIdx, CURRENT_PERIOD_COL)));
                row.setAccumulatedAmount(ParserValueUtils.toBigDecimal(getCellText(cells, rowIdx, ACCUMULATED_COL)));
                rows.add(row);
            }

            if (rows.isEmpty()) {
                result.addIssue("INVALID_WORKBOOK: no PL line records found");
                return result;
            }
            result.setData(rows);
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private String getCellText(Cells cells, int row, int col) {
        Cell cell = cells.get(row, col);
        if (cell == null || cell.getValue() == null) {
            return "";
        }
        return cell.getStringValue();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\u00A0", " ").trim();
    }

    private boolean isDigits(String value) {
        if (value == null || value.trim().isEmpty()) {
            return false;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
