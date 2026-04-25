package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
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
public class TaxAccountingDifferenceMonitorSheetParser implements SheetParser<List<TaxAccountingDifferenceMonitor23202355ItemDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.TAX_ACCOUNTING_DIFFERENCE_MONITOR;
    }

    @Override
    public Class<List<TaxAccountingDifferenceMonitor23202355ItemDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<TaxAccountingDifferenceMonitor23202355ItemDTO>> cls = (Class<List<TaxAccountingDifferenceMonitor23202355ItemDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<TaxAccountingDifferenceMonitor23202355ItemDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<TaxAccountingDifferenceMonitor23202355ItemDTO>> result = ParseResult.<List<TaxAccountingDifferenceMonitor23202355ItemDTO>>builder()
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
                result.addIssue("INVALID_WORKBOOK: 账税差异监控 未识别到表头");
                return result;
            }
            int titleRow = Math.max(0, headerRow - 1);

            int periodCol = findCol(cells, headerRow, maxCol, "日期", "所属期");
            int totalBookCol = findCol(cells, headerRow, maxCol, "汇总-账面收入");
            int totalDeclaredCol = findCol(cells, headerRow, maxCol, "汇总-增值税申报收入");
            int diffCol = findCol(cells, headerRow, maxCol, "账税差异");
            int reasonCol = findCol(cells, headerRow, maxCol, "差异分析");
            if (periodCol < 0 || totalBookCol < 0 || totalDeclaredCol < 0 || diffCol < 0 || reasonCol < 0) {
                result.addIssue("INVALID_WORKBOOK: 账税差异监控表头缺失");
                return result;
            }

            List<GroupCol> groupCols = new ArrayList<>();
            for (int col = periodCol + 1; col < totalBookCol; col++) {
                String subHeader = normalize(text(cells, headerRow, col));
                if (!subHeader.contains("账面收入")) {
                    continue;
                }
                String subHeaderNext = normalize(text(cells, headerRow, col + 1));
                if (!subHeaderNext.contains("申报收入")) {
                    continue;
                }
                String title = normalize(text(cells, titleRow, col));
                if (!StringUtils.hasText(title)) {
                    title = normalize(text(cells, titleRow, col + 1));
                }
                if (!StringUtils.hasText(title)) {
                    title = "分类" + (groupCols.size() + 1);
                }
                groupCols.add(new GroupCol(title, col, col + 1));
                col++;
            }
            if (groupCols.isEmpty()) {
                result.addIssue("INVALID_WORKBOOK: 账税差异监控未识别到分类分组");
                return result;
            }

            for (int row = headerRow + 1; row <= maxRow; row++) {
                String period = normalize(text(cells, row, periodCol));
                if (!StringUtils.hasText(period)) {
                    continue;
                }

                TaxAccountingDifferenceMonitor23202355ItemDTO item = new TaxAccountingDifferenceMonitor23202355ItemDTO();
                item.setPeriod(period);
                item.setCategoryIncomeList(new ArrayList<>());
                for (GroupCol group : groupCols) {
                    TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem groupItem =
                            new TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem();
                    groupItem.setTitle(group.title());
                    groupItem.setBookIncome(ParserValueUtils.toBigDecimal(text(cells, row, group.bookCol())));
                    groupItem.setDeclaredIncome(ParserValueUtils.toBigDecimal(text(cells, row, group.declaredCol())));
                    item.getCategoryIncomeList().add(groupItem);
                }
                item.setTotalBookIncome(ParserValueUtils.toBigDecimal(text(cells, row, totalBookCol)));
                item.setTotalVatDeclaredIncome(ParserValueUtils.toBigDecimal(text(cells, row, totalDeclaredCol)));
                item.setAccountingTaxDifference(ParserValueUtils.toBigDecimal(text(cells, row, diffCol)));
                item.setDifferenceAnalysis(normalize(text(cells, row, reasonCol)));
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
            if (findCol(cells, row, maxCol, "日期", "所属期") >= 0
                    && findCol(cells, row, maxCol, "汇总-账面收入") >= 0
                    && findCol(cells, row, maxCol, "汇总-增值税申报收入") >= 0) {
                return row;
            }
        }
        return -1;
    }

    private int findCol(Cells cells, int row, int maxCol, String... headers) {
        for (int col = 0; col <= maxCol; col++) {
            String value = normalize(text(cells, row, col));
            for (String header : headers) {
                String target = normalize(header);
                if (value.equals(target) || value.contains(target)) {
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
        return text == null ? "" : text.replace("\u00A0", " ").trim();
    }

    private record GroupCol(String title, int bookCol, int declaredCol) {
    }
}

