package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativeDeclarationSheetDTO;
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
public class ProjectCumulativeDeclarationSheetParser implements SheetParser<ProjectCumulativeDeclarationSheetDTO> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION;
    }

    @Override
    public Class<ProjectCumulativeDeclarationSheetDTO> resultType() {
        return ProjectCumulativeDeclarationSheetDTO.class;
    }

    @Override
    public ParseResult<ProjectCumulativeDeclarationSheetDTO> parse(InputStream inputStream, ParseContext context) {
        ProjectCumulativeDeclarationSheetDTO data = new ProjectCumulativeDeclarationSheetDTO();
        data.setRows(new ArrayList<>());
        ParseResult<ProjectCumulativeDeclarationSheetDTO> result = ParseResult.<ProjectCumulativeDeclarationSheetDTO>builder()
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
                result.addIssue("INVALID_WORKBOOK: 项目累计申报未识别到表头");
                return result;
            }

            int periodCol = findCol(cells, headerRow, maxCol, "所属期");
            int totalCol = findCol(cells, headerRow, maxCol, "税款合计");
            if (periodCol < 0 || totalCol < 0 || totalCol <= periodCol) {
                result.addIssue("INVALID_WORKBOOK: 项目累计申报表头缺失(所属期/税款合计)");
                return result;
            }

            for (int row = headerRow + 1; row <= maxRow; row++) {
                String period = normalize(text(cells, row, periodCol));
                if (!StringUtils.hasText(period)) {
                    continue;
                }
                List<ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO> rowCells = new ArrayList<>();
                for (int col = periodCol + 1; col <= totalCol; col++) {
                    String header = normalize(text(cells, headerRow, col));
                    if (!StringUtils.hasText(header)) {
                        continue;
                    }
                    ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO cell = new ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO();
                    cell.setHeaderName(header);
                    cell.setPeriod(period);
                    cell.setValue(ParserValueUtils.toBigDecimal(text(cells, row, col)));
                    rowCells.add(cell);
                }
                if (!rowCells.isEmpty()) {
                    data.getRows().add(rowCells);
                }
            }
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private int findHeaderRow(Cells cells, int maxRow, int maxCol) {
        for (int row = 0; row <= maxRow; row++) {
            int periodCol = findCol(cells, row, maxCol, "所属期");
            int totalCol = findCol(cells, row, maxCol, "税款合计");
            if (periodCol >= 0 && totalCol > periodCol) {
                return row;
            }
        }
        return -1;
    }

    private int findCol(Cells cells, int row, int maxCol, String header) {
        String target = normalize(header);
        for (int col = 0; col <= maxCol; col++) {
            if (target.equals(normalize(text(cells, row, col)))) {
                return col;
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

