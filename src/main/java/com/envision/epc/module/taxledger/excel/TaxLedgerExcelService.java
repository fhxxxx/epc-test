package com.envision.epc.module.taxledger.excel;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.domain.FileRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

/**
 * 台账 Excel 生成服务（Aspose.Cells）
 */
@Service
@RequiredArgsConstructor
public class TaxLedgerExcelService {
    private final SummaryTemplateRenderService summaryTemplateRenderService;

    /**
     * 生成最终台账（V1 最小可运行版）
     */
    public byte[] buildLedger(String companyCode,
                              String yearMonth,
                              List<FileRecord> files,
                              Map<String, Object> nodeOutputs,
                              SummarySheetDTO summaryData) throws Exception {
        Workbook workbook = summaryTemplateRenderService.render(summaryData);

        Worksheet stage1 = workbook.getWorksheets().add("Batch1_Direct");
        stage1.getCells().get("A1").putValue("Stage 1 direct sheets generated");
        Worksheet stage2 = workbook.getWorksheets().add("Batch2_Cumulative");
        stage2.getCells().get("A1").putValue("Stage 2 cumulative sheets generated");
        Worksheet stage3 = workbook.getWorksheets().add("Batch3_SummaryRef");
        stage3.getCells().get("A1").putValue("Stage 3 summary/ref generated");

        workbook.calculateFormula();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.save(outputStream, com.aspose.cells.SaveFormat.XLSX);
            return outputStream.toByteArray();
        }
    }
}
