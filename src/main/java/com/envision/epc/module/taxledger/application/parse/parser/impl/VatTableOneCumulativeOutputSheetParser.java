package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
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
public class VatTableOneCumulativeOutputSheetParser implements SheetParser<List<VatTableOneCumulativeOutputItemDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_TABLE_ONE_CUMULATIVE_OUTPUT;
    }

    @Override
    public Class<List<VatTableOneCumulativeOutputItemDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<VatTableOneCumulativeOutputItemDTO>> cls = (Class<List<VatTableOneCumulativeOutputItemDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<VatTableOneCumulativeOutputItemDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<VatTableOneCumulativeOutputItemDTO>> result = ParseResult.<List<VatTableOneCumulativeOutputItemDTO>>builder()
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
                result.addIssue("INVALID_WORKBOOK: 增值税表一 累计销项 未识别到表头");
                return result;
            }
            int titleRow = Math.max(0, headerRow - 1);
            int periodCol = findCol(cells, headerRow, maxCol, "所属期", "日期");
            int categoryCol = findCol(cells, headerRow, maxCol, "涉税类别");
            int invoicedAmountCol = findColByGroup(cells, titleRow, headerRow, maxCol, "已开票", "不含税金额");
            int invoicedTaxCol = findColByGroup(cells, titleRow, headerRow, maxCol, "已开票", "税额");
            int invoicedRateCol = findColByGroup(cells, titleRow, headerRow, maxCol, "已开票", "税率");
            int uninvoicedAmountCol = findColByGroup(cells, titleRow, headerRow, maxCol, "未开票", "不含税金额");
            int uninvoicedTaxCol = findColByGroup(cells, titleRow, headerRow, maxCol, "未开票", "税额");
            int uninvoicedRateCol = findColByGroup(cells, titleRow, headerRow, maxCol, "未开票", "税率");
            int totalAmountCol = findColByGroup(cells, titleRow, headerRow, maxCol, "合计", "不含税金额");
            int totalTaxCol = findColByGroup(cells, titleRow, headerRow, maxCol, "合计", "税额");

            if (periodCol < 0 || categoryCol < 0 || invoicedAmountCol < 0 || invoicedTaxCol < 0 || invoicedRateCol < 0
                    || uninvoicedAmountCol < 0 || uninvoicedTaxCol < 0 || uninvoicedRateCol < 0 || totalAmountCol < 0 || totalTaxCol < 0) {
                result.addIssue("INVALID_WORKBOOK: 增值税表一 累计销项表头缺失");
                return result;
            }

            for (int row = headerRow + 1; row <= maxRow; row++) {
                String period = normalize(text(cells, row, periodCol));
                String taxCategory = normalize(text(cells, row, categoryCol));
                if (!StringUtils.hasText(period) || !StringUtils.hasText(taxCategory)) {
                    continue;
                }
                VatTableOneCumulativeOutputItemDTO item = new VatTableOneCumulativeOutputItemDTO();
                item.setPeriod(period);
                item.setTaxCategory(taxCategory);
                item.setInvoicedAmountExclTax(ParserValueUtils.toBigDecimal(text(cells, row, invoicedAmountCol)));
                item.setInvoicedTaxAmount(ParserValueUtils.toBigDecimal(text(cells, row, invoicedTaxCol)));
                item.setInvoicedTaxRate(ParserValueUtils.toBigDecimal(text(cells, row, invoicedRateCol)));
                item.setUninvoicedAmountExclTax(ParserValueUtils.toBigDecimal(text(cells, row, uninvoicedAmountCol)));
                item.setUninvoicedTaxAmount(ParserValueUtils.toBigDecimal(text(cells, row, uninvoicedTaxCol)));
                item.setUninvoicedTaxRate(ParserValueUtils.toBigDecimal(text(cells, row, uninvoicedRateCol)));
                item.setTotalAmountExclTax(ParserValueUtils.toBigDecimal(text(cells, row, totalAmountCol)));
                item.setTotalTaxAmount(ParserValueUtils.toBigDecimal(text(cells, row, totalTaxCol)));
                result.getData().add(item);
            }
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private int findHeaderRow(Cells cells, int maxRow, int maxCol) {
        for (int row = 0; row <= maxRow; row++) {
            if (findCol(cells, row, maxCol, "所属期", "日期") >= 0
                    && findCol(cells, row, maxCol, "涉税类别") >= 0
                    && findCol(cells, row, maxCol, "不含税金额") >= 0
                    && findCol(cells, row, maxCol, "税额") >= 0) {
                return row;
            }
        }
        return -1;
    }

    private int findColByGroup(Cells cells, int titleRow, int headerRow, int maxCol, String group, String subHeader) {
        for (int col = 0; col <= maxCol; col++) {
            String groupText = normalize(text(cells, titleRow, col));
            String headerText = normalize(text(cells, headerRow, col));
            if (groupText.contains(normalize(group)) && headerText.contains(normalize(subHeader))) {
                return col;
            }
        }
        return findCol(cells, headerRow, maxCol, group + "-" + subHeader, subHeader);
    }

    private int findCol(Cells cells, int row, int maxCol, String... candidates) {
        for (int col = 0; col <= maxCol; col++) {
            String val = normalize(text(cells, row, col));
            for (String candidate : candidates) {
                String key = normalize(candidate);
                if (val.equals(key) || val.contains(key)) {
                    return col;
                }
            }
        }
        return -1;
    }

    private String text(Cells cells, int row, int col) {
        Cell cell = cells.get(row, col);
        if (cell == null || cell.getValue() == null) {
            return "";
        }
        return cell.getStringValue();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\u00A0", " ").replace("：", ":").trim();
    }
}
