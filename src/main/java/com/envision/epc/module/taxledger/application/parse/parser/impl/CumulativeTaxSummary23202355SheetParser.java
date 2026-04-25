package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CumulativeTaxSummary23202355SheetParser implements SheetParser<List<CumulativeTaxSummary23202355ColumnDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.CUMULATIVE_TAX_SUMMARY_2320_2355;
    }

    @Override
    public Class<List<CumulativeTaxSummary23202355ColumnDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<CumulativeTaxSummary23202355ColumnDTO>> cls = (Class<List<CumulativeTaxSummary23202355ColumnDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<CumulativeTaxSummary23202355ColumnDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<CumulativeTaxSummary23202355ColumnDTO>> result = ParseResult.<List<CumulativeTaxSummary23202355ColumnDTO>>builder()
                .data(new ArrayList<>())
                .build();
        try {
            Workbook workbook = new Workbook(inputStream);
            Worksheet sheet = SheetSelectUtils.resolveAsposeSheet(workbook, category());
            Cells cells = sheet.getCells();
            int maxRow = cells.getMaxDataRow();
            int maxCol = cells.getMaxDataColumn();

            int metricRow = findRowContains(cells, maxRow, maxCol, "账面收入");
            if (metricRow < 0) {
                result.addIssue("INVALID_WORKBOOK: 累计税金汇总表未识别到指标行");
                return result;
            }
            int metricCol = findColContains(cells, metricRow, maxCol, "账面收入");
            int periodHeaderRow = findPeriodHeaderRow(cells, metricRow, maxCol, metricCol + 1);
            if (periodHeaderRow < 0) {
                result.addIssue("INVALID_WORKBOOK: 累计税金汇总表未识别到期间表头");
                return result;
            }

            List<Integer> periodCols = new ArrayList<>();
            List<String> periods = new ArrayList<>();
            for (int col = metricCol + 1; col <= maxCol; col++) {
                String value = normalize(text(cells, periodHeaderRow, col));
                if (isPeriod(value)) {
                    periodCols.add(col);
                    periods.add(value);
                }
            }
            if (periodCols.isEmpty()) {
                result.addIssue("INVALID_WORKBOOK: 累计税金汇总表期间列为空");
                return result;
            }

            Map<String, Integer> rowMap = buildMetricRowMap(cells, maxRow, metricCol);
            for (int i = 0; i < periodCols.size(); i++) {
                int col = periodCols.get(i);
                CumulativeTaxSummary23202355ColumnDTO dto = new CumulativeTaxSummary23202355ColumnDTO();
                dto.setPeriod(periods.get(i));
                dto.setBookIncome(read(cells, rowMap, "账面收入", col));
                dto.setOtherIncome(read(cells, rowMap, "其他收益", col));
                dto.setProjectInvoicing(read(cells, rowMap, "项目开票", col));
                dto.setInterestInvoicing(read(cells, rowMap, "利息开票", col));
                dto.setOutputTaxA(read(cells, rowMap, "销项税额(A)", col));
                dto.setCurrentInputTaxB(read(cells, rowMap, "本期进项税额(B)", col));
                dto.setOpeningRetainedInputTaxC(read(cells, rowMap, "上期留抵税额(C)", col));
                dto.setInputTaxTransferOutD(read(cells, rowMap, "进项税额转出(D)", col));
                dto.setRemotePrepaidVatE(read(cells, rowMap, "异地预缴税增值税(E)", col));
                dto.setVatPayableAminusBminusCplusDminusE(read(cells, rowMap, "应纳增值税额(A-B-C+D-E)", col));
                dto.setVatAmount(read(cells, rowMap, "增值税", col));
                dto.setStampDuty(read(cells, rowMap, "印花税", col));
                dto.setUrbanConstructionTax(read(cells, rowMap, "城建税", col));
                dto.setEducationSurcharge(read(cells, rowMap, "教育费附加", col));
                dto.setLocalEducationSurcharge(read(cells, rowMap, "地方教育费附加", col));
                dto.setPropertyTax(read(cells, rowMap, "房产税", col));
                dto.setUrbanLandUseTax(read(cells, rowMap, "城镇土地使用", col));
                dto.setCorporateIncomeTax(read(cells, rowMap, "企业所得税", col));
                dto.setIndividualIncomeTax(read(cells, rowMap, "个税", col));
                dto.setDisabledPersonsEmploymentSecurityFund(read(cells, rowMap, "残疾人保障金", col));
                dto.setTotalTaxAmount(read(cells, rowMap, "合计", col));
                result.getData().add(dto);
            }
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private Map<String, Integer> buildMetricRowMap(Cells cells, int maxRow, int metricCol) {
        Map<String, Integer> map = new HashMap<>();
        for (int row = 0; row <= maxRow; row++) {
            String metric = normalize(text(cells, row, metricCol));
            if (!StringUtils.hasText(metric)) {
                continue;
            }
            map.putIfAbsent(metric, row);
        }
        return map;
    }

    private java.math.BigDecimal read(Cells cells, Map<String, Integer> rowMap, String metric, int col) {
        Integer row = rowMap.get(metric);
        if (row == null && "异地预缴税增值税(E)".equals(metric)) {
            row = rowMap.get("异地预缴递减");
        }
        if (row == null) {
            return null;
        }
        return ParserValueUtils.toBigDecimal(text(cells, row, col));
    }

    private int findPeriodHeaderRow(Cells cells, int maxRowSearch, int maxCol, int startCol) {
        int bestRow = -1;
        int bestCount = 0;
        for (int row = 0; row <= maxRowSearch; row++) {
            int count = 0;
            for (int col = startCol; col <= maxCol; col++) {
                if (isPeriod(normalize(text(cells, row, col)))) {
                    count++;
                }
            }
            if (count > bestCount) {
                bestCount = count;
                bestRow = row;
            }
        }
        return bestCount > 0 ? bestRow : -1;
    }

    private boolean isPeriod(String text) {
        return text.matches("^\\d{6}$") || text.matches("^\\d{4}-\\d{2}$");
    }

    private int findRowContains(Cells cells, int maxRow, int maxCol, String target) {
        for (int row = 0; row <= maxRow; row++) {
            for (int col = 0; col <= maxCol; col++) {
                if (normalize(text(cells, row, col)).contains(target)) {
                    return row;
                }
            }
        }
        return -1;
    }

    private int findColContains(Cells cells, int row, int maxCol, String target) {
        for (int col = 0; col <= maxCol; col++) {
            if (normalize(text(cells, row, col)).contains(target)) {
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

