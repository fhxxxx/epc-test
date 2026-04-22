package com.envision.epc.module.taxledger.excel;

import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

class SummaryTemplateRenderServiceTest {

    private final SummaryTemplateRenderService service = new SummaryTemplateRenderService();

    @Test
    void shouldRenderSummaryWithoutTemplateSheets() throws Exception {
        SummarySheetDTO dto = new SummarySheetDTO();
        dto.setCompanyName("1001");
        dto.setLedgerPeriod("2026-03");
        dto.setStampDutyRows(List.of(stamp(1, "购销合同", new BigDecimal("10000"), new BigDecimal("12.5"))));
        dto.setCommonTaxRows(List.of());
        dto.setCorporateIncomeTaxRows(List.of());
        SummarySheetDTO.FinalTotalItem total = new SummarySheetDTO.FinalTotalItem();
        total.setTotalTitle("合计");
        total.setDeclaredTotal(new BigDecimal("12.5"));
        total.setBookTotal(BigDecimal.ZERO);
        dto.setFinalTotal(total);

        Workbook workbook = service.render(dto);
        Assertions.assertEquals(1, workbook.getWorksheets().getCount());
        Assertions.assertNotNull(workbook.getWorksheets().get("Summary"));

        Worksheet summary = workbook.getWorksheets().get("Summary");
        Cells cells = summary.getCells();
        Assertions.assertTrue(containsText(cells, "1001 税务台账"));
        Assertions.assertTrue(containsText(cells, "购销合同"));
        Assertions.assertTrue(containsText(cells, "小计"));
        Assertions.assertTrue(containsText(cells, "合计"));
    }

    @Test
    void shouldSkipAllSectionsWhenRowsEmpty() throws Exception {
        SummarySheetDTO dto = new SummarySheetDTO();
        dto.setCompanyName("2001");
        dto.setLedgerPeriod("2026-04");
        dto.setStampDutyRows(List.of());
        dto.setCommonTaxRows(List.of());
        dto.setCorporateIncomeTaxRows(List.of());
        SummarySheetDTO.FinalTotalItem total = new SummarySheetDTO.FinalTotalItem();
        total.setTotalTitle("合计");
        total.setDeclaredTotal(BigDecimal.ZERO);
        total.setBookTotal(BigDecimal.ZERO);
        dto.setFinalTotal(total);

        Workbook workbook = service.render(dto);
        Worksheet summary = workbook.getWorksheets().get("Summary");
        Cells cells = summary.getCells();
        Assertions.assertTrue(containsText(cells, "合计"));
        Assertions.assertFalse(containsText(cells, "小计"));
    }

    private SummarySheetDTO.StampDutyItem stamp(Integer seq, String item, BigDecimal base, BigDecimal declared) {
        SummarySheetDTO.StampDutyItem row = new SummarySheetDTO.StampDutyItem();
        row.setSeqNo(seq);
        row.setTaxType("印花税");
        row.setTaxItem(item);
        row.setTaxBasisDesc("合同计税");
        row.setTaxBaseQuarter(base);
        row.setTaxRate(new BigDecimal("0.001"));
        row.setActualTaxPayable(declared);
        return row;
    }

    private boolean containsText(Cells cells, String expected) {
        int maxRow = cells.getMaxDataRow();
        int maxCol = cells.getMaxDataColumn();
        for (int row = 0; row <= maxRow; row++) {
            for (int col = 0; col <= maxCol; col++) {
                String text = cells.get(row, col).getStringValue();
                if (expected.equals(text) || (text != null && text.contains(expected))) {
                    return true;
                }
            }
        }
        return false;
    }
}
