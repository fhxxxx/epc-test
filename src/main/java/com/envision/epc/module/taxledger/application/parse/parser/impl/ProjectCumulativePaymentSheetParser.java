package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativePaymentSheetDTO;
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
public class ProjectCumulativePaymentSheetParser implements SheetParser<ProjectCumulativePaymentSheetDTO> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT;
    }

    @Override
    public Class<ProjectCumulativePaymentSheetDTO> resultType() {
        return ProjectCumulativePaymentSheetDTO.class;
    }

    @Override
    public ParseResult<ProjectCumulativePaymentSheetDTO> parse(InputStream inputStream, ParseContext context) {
        ProjectCumulativePaymentSheetDTO data = new ProjectCumulativePaymentSheetDTO();
        data.setRows(new ArrayList<>());
        ParseResult<ProjectCumulativePaymentSheetDTO> result = ParseResult.<ProjectCumulativePaymentSheetDTO>builder()
                .data(data)
                .build();
        try {
            Workbook workbook = new Workbook(inputStream);
            Worksheet sheet = SheetSelectUtils.resolveAsposeSheet(workbook, category());
            Cells cells = sheet.getCells();
            int maxRow = cells.getMaxDataRow();
            int maxCol = cells.getMaxDataColumn();

            int headerRow = findHeaderRow(cells, maxRow, maxCol);
            if (headerRow < 0) {
                result.addIssue("INVALID_WORKBOOK: 项目累计缴纳未识别到表头");
                return result;
            }

            int periodCol = findCol(cells, headerRow, maxCol, "实际缴纳所属期", "所属期");
            int paidCol = findCol(cells, headerRow, maxCol, "实缴税金");
            int declaredCol = findCol(cells, headerRow, maxCol, "申请税金");
            int diffCol = findCol(cells, headerRow, maxCol, "差异");
            int reasonCol = findCol(cells, headerRow, maxCol, "原因");
            if (periodCol < 0 || paidCol < 0 || declaredCol < 0 || diffCol < 0 || reasonCol < 0) {
                result.addIssue("INVALID_WORKBOOK: 项目累计缴纳表头缺失(所属期/实缴税金/申请税金/差异/原因)");
                return result;
            }

            for (int row = headerRow + 1; row <= maxRow; row++) {
                String period = normalize(text(cells, row, periodCol));
                if (!StringUtils.hasText(period)) {
                    continue;
                }
                ProjectCumulativePaymentSheetDTO.ProjectCumulativePaymentRowDTO rowDTO =
                        new ProjectCumulativePaymentSheetDTO.ProjectCumulativePaymentRowDTO();
                rowDTO.setPeriod(period);
                rowDTO.setDynamicTaxCells(new ArrayList<>());

                for (int col = periodCol + 1; col < paidCol; col++) {
                    String header = normalize(text(cells, headerRow, col));
                    if (!StringUtils.hasText(header)) {
                        continue;
                    }
                    ProjectCumulativePaymentSheetDTO.ProjectTaxCellDTO cell = new ProjectCumulativePaymentSheetDTO.ProjectTaxCellDTO();
                    cell.setHeaderName(header);
                    cell.setValue(ParserValueUtils.toBigDecimal(text(cells, row, col)));
                    rowDTO.getDynamicTaxCells().add(cell);
                }

                rowDTO.setPaidTotal(ParserValueUtils.toBigDecimal(text(cells, row, paidCol)));
                rowDTO.setDeclaredTotal(ParserValueUtils.toBigDecimal(text(cells, row, declaredCol)));
                rowDTO.setDifferenceAmount(ParserValueUtils.toBigDecimal(text(cells, row, diffCol)));
                rowDTO.setReason(normalize(text(cells, row, reasonCol)));
                data.getRows().add(rowDTO);
            }
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private int findHeaderRow(Cells cells, int maxRow, int maxCol) {
        for (int row = 0; row <= maxRow; row++) {
            int periodCol = findCol(cells, row, maxCol, "实际缴纳所属期", "所属期");
            int paidCol = findCol(cells, row, maxCol, "实缴税金");
            int declaredCol = findCol(cells, row, maxCol, "申请税金");
            if (periodCol >= 0 && paidCol > periodCol && declaredCol > paidCol) {
                return row;
            }
        }
        return -1;
    }

    private int findCol(Cells cells, int row, int maxCol, String... headers) {
        for (int col = 0; col <= maxCol; col++) {
            String value = normalize(text(cells, row, col));
            for (String header : headers) {
                if (normalize(header).equals(value)) {
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
}

