package com.envision.epc.module.taxledger.excel;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.domain.TaxFileRecord;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 台账 Excel 生成服务（Aspose.Cells）
 */
@Service
public class TaxLedgerExcelService {
    /**
     * 生成最终台账（V1 最小可运行版）
     */
    public byte[] buildLedger(String companyCode, String yearMonth, List<TaxFileRecord> files) throws Exception {
        Workbook workbook = new Workbook();
        workbook.getWorksheets().get(0).setName("Summary");
        Worksheet summary = workbook.getWorksheets().get("Summary");

        summary.getCells().get("A1").putValue("Company");
        summary.getCells().get("B1").putValue(companyCode);
        summary.getCells().get("A2").putValue("YearMonth");
        summary.getCells().get("B2").putValue(yearMonth);
        summary.getCells().get("A3").putValue("GeneratedAt");
        summary.getCells().get("B3").putValue(LocalDateTime.now().toString());

        summary.getCells().get("A5").putValue("Input Category");
        summary.getCells().get("B5").putValue("File Name");
        summary.getCells().get("C5").putValue("File Size");

        int row = 6;
        for (TaxFileRecord file : files) {
            summary.getCells().get(row, 0).putValue(file.getFileCategory().name());
            summary.getCells().get(row, 1).putValue(file.getFileName());
            summary.getCells().get(row, 2).putValue(file.getFileSize() == null ? 0 : file.getFileSize());
            row++;
        }

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
