package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.envision.epc.module.taxledger.application.dto.MonthlySettlementTaxParsedDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonthlySettlementTaxSheetParserTest {
    private final MonthlySettlementTaxSheetParser parser = new MonthlySettlementTaxSheetParser();

    @Test
    void shouldAggregateByRateAndSkipTotalRow() throws IOException {
        ParseResult<MonthlySettlementTaxParsedDTO> result = parser.parse(
                new ByteArrayInputStream(buildWorkbookBytes("设计服务6.0%", "咨询服务-在建9%")),
                context());

        assertNotNull(result);
        assertFalse(result.hasError());
        assertNotNull(result.getData());
        MonthlySettlementTaxParsedDTO data = result.getData();
        assertEquals(2, data.getAggregateByRate().size());

        MonthlySettlementTaxParsedDTO.RateAggregate six = data.getAggregateByRate().get("6%");
        assertNotNull(six);
        assertEquals(0, six.getIncomeSum().compareTo(new BigDecimal("100")));
        assertEquals(0, six.getOutputTaxSum().compareTo(new BigDecimal("6")));
        assertEquals(0, six.getInvoicedIncomeSum().compareTo(new BigDecimal("80")));
        assertEquals(0, six.getInvoicedTaxAmountSum().compareTo(new BigDecimal("4.8")));

        MonthlySettlementTaxParsedDTO.RateAggregate nine = data.getAggregateByRate().get("9%");
        assertNotNull(nine);
        assertEquals(0, nine.getIncomeSum().compareTo(new BigDecimal("200")));
        assertEquals(0, nine.getOutputTaxSum().compareTo(new BigDecimal("18")));
        assertEquals(0, nine.getInvoicedIncomeSum().compareTo(new BigDecimal("150")));
        assertEquals(0, nine.getInvoicedTaxAmountSum().compareTo(new BigDecimal("13.5")));
    }

    @Test
    void shouldSkipSectionWhenTitleHasNoTaxRate() throws IOException {
        ParseResult<MonthlySettlementTaxParsedDTO> result = parser.parse(
                new ByteArrayInputStream(buildWorkbookBytes("设计服务6%", "咨询服务-在建")),
                context());

        assertNotNull(result);
        assertNotNull(result.getData());
        assertTrue(result.hasError());
        assertEquals(1, result.getData().getAggregateByRate().size());
        assertNotNull(result.getData().getAggregateByRate().get("6%"));
        assertTrue(result.getIssues().stream().anyMatch(msg -> msg.contains("分段标题无法提取税率")));
    }

    @Test
    void shouldReturnIssueWhenInputStreamIsNull() {
        ParseResult<MonthlySettlementTaxParsedDTO> result = parser.parse(null, context());
        assertNotNull(result);
        assertNotNull(result.getData());
        assertTrue(result.hasError());
        assertTrue(result.getData().getAggregateByRate().isEmpty());
    }

    private static ParseContext context() {
        return ParseContext.builder()
                .companyCode("2320")
                .yearMonth("2026-03")
                .fileName("monthly.xlsx")
                .operator("tester")
                .traceId("t-1")
                .build();
    }

    private static byte[] buildWorkbookBytes(String section1Title, String section2Title) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(FileCategoryEnum.MONTHLY_SETTLEMENT_TAX.getTargetSheetName());

            Row titleRow = sheet.createRow(0);
            titleRow.createCell(3).setCellValue(section1Title);
            titleRow.createCell(10).setCellValue(section2Title);

            Row header = sheet.createRow(1);
            writeSectionHeader(header, 3);
            writeSectionHeader(header, 10);

            Row data = sheet.createRow(2);
            data.createCell(0).setCellValue("项目A");
            data.createCell(3).setCellValue("明细");
            data.createCell(4).setCellValue("100");
            data.createCell(5).setCellValue("6");
            data.createCell(6).setCellValue("80");
            data.createCell(7).setCellValue("4.8");
            data.createCell(8).setCellValue("20");
            data.createCell(9).setCellValue("1.2");
            data.createCell(10).setCellValue("明细");
            data.createCell(11).setCellValue("200");
            data.createCell(12).setCellValue("18");
            data.createCell(13).setCellValue("150");
            data.createCell(14).setCellValue("13.5");
            data.createCell(15).setCellValue("50");
            data.createCell(16).setCellValue("4.5");

            Row total = sheet.createRow(3);
            total.createCell(0).setCellValue("项目A");
            total.createCell(3).setCellValue("合计");
            total.createCell(4).setCellValue("100");
            total.createCell(5).setCellValue("6");
            total.createCell(6).setCellValue("80");
            total.createCell(7).setCellValue("4.8");
            total.createCell(8).setCellValue("20");
            total.createCell(9).setCellValue("1.2");
            total.createCell(10).setCellValue("合计");
            total.createCell(11).setCellValue("200");
            total.createCell(12).setCellValue("18");
            total.createCell(13).setCellValue("150");
            total.createCell(14).setCellValue("13.5");
            total.createCell(15).setCellValue("50");
            total.createCell(16).setCellValue("4.5");

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void writeSectionHeader(Row header, int startCol) {
        header.createCell(startCol).setCellValue("成本");
        header.createCell(startCol + 1).setCellValue("收入");
        header.createCell(startCol + 2).setCellValue("销项");
        header.createCell(startCol + 3).setCellValue("已开票收入");
        header.createCell(startCol + 4).setCellValue("已开票税额");
        header.createCell(startCol + 5).setCellValue("未开票收入");
        header.createCell(startCol + 6).setCellValue("未开票税额");
    }
}
