package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
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
public class UninvoicedMonitorSheetParser implements SheetParser<List<UninvoicedMonitorItemDTO>> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.UNINVOICED_MONITOR;
    }

    @Override
    public Class<List<UninvoicedMonitorItemDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<UninvoicedMonitorItemDTO>> cls = (Class<List<UninvoicedMonitorItemDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<UninvoicedMonitorItemDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<UninvoicedMonitorItemDTO>> result = ParseResult.<List<UninvoicedMonitorItemDTO>>builder()
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
                result.addIssue("INVALID_WORKBOOK: 未开票数监控 未识别到表头");
                return result;
            }

            int periodCol = findCol(cells, headerRow, maxCol, "所属期", "日期");
            int declaredMainCol = findCol(cells, headerRow, maxCol, "申报账面数-营业收入", "营业收入");
            int declaredInterestCol = findCol(cells, headerRow, maxCol, "申报账面数-利息收入", "利息收入");
            int declaredOtherCol = findCol(cells, headerRow, maxCol, "申报账面数-其他收益", "其他收益");
            int declaredOutputTaxCol = findCol(cells, headerRow, maxCol, "申报账面数-销项税额", "销项税额");
            int invoicedSalesCol = findCol(cells, headerRow, maxCol, "开票数-销售收入");
            int invoicedOutputTaxCol = findCol(cells, headerRow, maxCol, "开票数-销项税额");
            int uninvoicedSalesCol = findCol(cells, headerRow, maxCol, "未开票数-销售收入");
            int uninvoicedOutputTaxCol = findCol(cells, headerRow, maxCol, "未开票数-销项税额");

            if (periodCol < 0 || declaredMainCol < 0 || declaredOutputTaxCol < 0 || invoicedSalesCol < 0
                    || invoicedOutputTaxCol < 0 || uninvoicedSalesCol < 0 || uninvoicedOutputTaxCol < 0) {
                result.addIssue("INVALID_WORKBOOK: 未开票数监控表头缺失");
                return result;
            }

            for (int row = headerRow + 1; row <= maxRow; row++) {
                String period = normalize(text(cells, row, periodCol));
                if (!StringUtils.hasText(period) || period.contains("合计")) {
                    continue;
                }
                UninvoicedMonitorItemDTO item = new UninvoicedMonitorItemDTO();
                item.setPeriod(period);
                item.setDeclaredMainBusinessRevenue(ParserValueUtils.toBigDecimal(text(cells, row, declaredMainCol)));
                item.setDeclaredInterestIncome(ParserValueUtils.toBigDecimal(text(cells, row, declaredInterestCol)));
                item.setDeclaredOtherIncome(ParserValueUtils.toBigDecimal(text(cells, row, declaredOtherCol)));
                item.setDeclaredOutputTax(ParserValueUtils.toBigDecimal(text(cells, row, declaredOutputTaxCol)));
                item.setInvoicedSalesIncome(ParserValueUtils.toBigDecimal(text(cells, row, invoicedSalesCol)));
                item.setInvoicedOutputTax(ParserValueUtils.toBigDecimal(text(cells, row, invoicedOutputTaxCol)));
                item.setUninvoicedSalesIncome(ParserValueUtils.toBigDecimal(text(cells, row, uninvoicedSalesCol)));
                item.setUninvoicedOutputTax(ParserValueUtils.toBigDecimal(text(cells, row, uninvoicedOutputTaxCol)));
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
                    && findCol(cells, row, maxCol, "未开票数-销售收入") >= 0
                    && findCol(cells, row, maxCol, "未开票数-销项税额") >= 0) {
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

