package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.SheetSelectUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class VatChangeSheetParser implements SheetParser<List<VatChangeRowDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_CHANGE;
    }

    @Override
    public Class<List<VatChangeRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<VatChangeRowDTO>> cls = (Class<List<VatChangeRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<VatChangeRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<VatChangeRowDTO>> result = ParseResult.<List<VatChangeRowDTO>>builder()
                .data(new ArrayList<>())
                .build();
        try {
            Workbook workbook = new Workbook(inputStream);
            Worksheet sheet = SheetSelectUtils.resolveAsposeSheet(workbook, category());
            Cells cells = sheet.getCells();
            int maxRow = cells.getMaxDataRow();
            int maxCol = cells.getMaxDataColumn();

            int headerRow = findHeaderRow(cells, maxRow, maxCol);
            if (headerRow < 0) {
                result.addIssue("INVALID_WORKBOOK: 增值税变动表 未识别到表头");
                return result;
            }

            int baseItemCol = findCol(cells, headerRow, maxCol, "基础条目");
            int splitBasisCol = findCol(cells, headerRow, maxCol, "拆分依据");
            int itemNameCol = findCol(cells, headerRow, maxCol, "条目");
            int unbilledCol = findCol(cells, headerRow, maxCol, "未开票金额");
            int currentInvoicedCol = findCol(cells, headerRow, maxCol, "当月开票金额");
            int prevInvoicedCol = findCol(cells, headerRow, maxCol, "以前月度开票金额");
            int totalCol = findCol(cells, headerRow, maxCol, "合计");

            if (baseItemCol < 0 || totalCol < 0) {
                result.addIssue("INVALID_WORKBOOK: 增值税变动表表头缺失(基础条目/合计)");
                return result;
            }

            for (int row = headerRow + 1; row <= maxRow; row++) {
                String baseItem = normalize(text(cells, row, baseItemCol));
                if (!StringUtils.hasText(baseItem)) {
                    continue;
                }
                VatChangeRowDTO dto = new VatChangeRowDTO();
                dto.setBaseItem(baseItem);
                dto.setSplitBasis(getIfValid(cells, row, splitBasisCol));
                dto.setItemName(getIfValid(cells, row, itemNameCol));
                dto.setUnbilledAmount(parseIfValid(cells, row, unbilledCol));
                dto.setCurrentMonthInvoicedAmount(parseIfValid(cells, row, currentInvoicedCol));
                dto.setPreviousMonthInvoicedAmount(parseIfValid(cells, row, prevInvoicedCol));
                dto.setTotalAmount(parseIfValid(cells, row, totalCol));
                result.getData().add(dto);
            }
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private int findHeaderRow(Cells cells, int maxRow, int maxCol) {
        for (int row = 0; row <= maxRow; row++) {
            if (findCol(cells, row, maxCol, "基础条目") >= 0 && findCol(cells, row, maxCol, "合计") >= 0) {
                return row;
            }
        }
        return -1;
    }

    private int findCol(Cells cells, int row, int maxCol, String... candidates) {
        for (int col = 0; col <= maxCol; col++) {
            String value = normalize(text(cells, row, col));
            for (String candidate : candidates) {
                String target = normalize(candidate);
                if (value.equals(target) || value.contains(target)) {
                    return col;
                }
            }
        }
        return -1;
    }

    private String getIfValid(Cells cells, int row, int col) {
        if (col < 0) {
            return null;
        }
        String value = normalize(text(cells, row, col));
        return StringUtils.hasText(value) ? value : null;
    }

    private java.math.BigDecimal parseIfValid(Cells cells, int row, int col) {
        if (col < 0) {
            return null;
        }
        return ParserValueUtils.toBigDecimal(text(cells, row, col));
    }

    private String text(Cells cells, int row, int col) {
        Cell cell = cells.get(row, col);
        if (cell == null || cell.getValue() == null) {
            return "";
        }
        return cell.getStringValue();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\u00A0", " ").trim();
    }
}

