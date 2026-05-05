package com.envision.epc.module.taxledger.excel;

import com.aspose.cells.SaveFormat;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRegistry;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.LedgerWorkbookData;
import com.envision.epc.module.taxledger.application.ledger.SheetExecutionPlan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 台账 Excel 组装服务（Aspose.Cells）
 */
@Service
@RequiredArgsConstructor
public class TaxLedgerExcelService {
    private final SheetExecutionPlan executionPlan;
    private final LedgerSheetRegistry registry;

    public LedgerBuildOutput buildLedger(LedgerWorkbookData workbookData,
                                         LedgerRenderContext renderContext) throws Exception {
        long renderStart = System.currentTimeMillis();
        Workbook workbook = new Workbook();
        Map<LedgerSheetCode, Map<String, Object>> renderReportsByCode = new LinkedHashMap<>();
        Map<LedgerSheetCode, List<String>> renderedSheetNamesByCode = new LinkedHashMap<>();
        List<LedgerSheetCode> renderExecutionOrder = executionPlan.orderedForRenderExecution(renderContext.getCompanyCode());
        List<LedgerSheetCode> displayOrder = executionPlan.orderedForDisplay(renderContext.getCompanyCode());

        for (LedgerSheetCode code : renderExecutionOrder) {
            LedgerSheetData data = workbookData.required(code);
            @SuppressWarnings("unchecked")
            LedgerSheetRenderer<LedgerSheetData> renderer =
                    (LedgerSheetRenderer<LedgerSheetData>) registry.requiredRenderer(code);

            int beforeCount = workbook.getWorksheets().getCount();
            long start = System.currentTimeMillis();
            renderer.render(workbook, data, renderContext);
            long elapsed = System.currentTimeMillis() - start;
            List<String> renderedSheetNames = captureRenderedSheetNames(workbook, beforeCount);
            renderedSheetNamesByCode.put(code, renderedSheetNames);

            Map<String, Object> sheetReport = new LinkedHashMap<>();
            sheetReport.put("sheetCode", code.name());
            sheetReport.put("sheetName", renderedSheetNames.isEmpty() ? code.getSheetName() : renderedSheetNames.get(0));
            sheetReport.put("displayName", code.getDisplayName());
            sheetReport.put("mode", code.getMode().name());
            sheetReport.put("buildMs", workbookData.getBuildCostMs() == null ? null : workbookData.getBuildCostMs().get(code));
            sheetReport.put("renderMs", elapsed);
            sheetReport.put("rowCount", data.rowCount());
            renderReportsByCode.put(code, sheetReport);
        }

        removeDefaultBlankSheet(workbook);
        reorderWorksheetsByDisplayOrder(workbook, displayOrder, renderedSheetNamesByCode);
        workbook.calculateFormula();

        List<Map<String, Object>> sheetReports = new ArrayList<>();
        for (LedgerSheetCode code : displayOrder) {
            Map<String, Object> report = renderReportsByCode.get(code);
            if (report != null) {
                sheetReports.add(report);
            }
        }

        byte[] bytes;
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            workbook.save(outputStream, SaveFormat.XLSX);
            bytes = outputStream.toByteArray();
        }

        Map<String, Object> buildReport = new LinkedHashMap<>();
        buildReport.put("generatedAt", LocalDateTime.now().toString());
        buildReport.put("companyCode", renderContext.getCompanyCode());
        buildReport.put("yearMonth", renderContext.getYearMonth());
        buildReport.put("sheetCount", sheetReports.size());
        buildReport.put("totalRenderMs", System.currentTimeMillis() - renderStart);
        buildReport.put("sheets", sheetReports);

        return LedgerBuildOutput.builder()
                .workbookBytes(bytes)
                .buildReport(buildReport)
                .build();
    }

    private void reorderWorksheetsByDisplayOrder(Workbook workbook,
                                                 List<LedgerSheetCode> displayOrder,
                                                 Map<LedgerSheetCode, List<String>> renderedSheetNamesByCode) {
        int targetIndex = 0;
        for (LedgerSheetCode code : displayOrder) {
            List<String> candidates = renderedSheetNamesByCode == null ? List.of() : renderedSheetNamesByCode.get(code);
            if (candidates == null || candidates.isEmpty()) {
                candidates = List.of(code.getSheetName());
            }
            for (String sheetName : candidates) {
                Worksheet sheet = workbook.getWorksheets().get(sheetName);
                if (sheet == null) {
                    continue;
                }
                sheet.moveTo(targetIndex++);
            }
        }
    }

    private List<String> captureRenderedSheetNames(Workbook workbook, int beforeCount) {
        int afterCount = workbook.getWorksheets().getCount();
        if (afterCount <= beforeCount) {
            return List.of();
        }
        List<String> names = new ArrayList<>();
        for (int i = beforeCount; i < afterCount; i++) {
            Worksheet sheet = workbook.getWorksheets().get(i);
            if (sheet != null && sheet.getName() != null) {
                names.add(sheet.getName());
            }
        }
        return names;
    }

    private void removeDefaultBlankSheet(Workbook workbook) {
        if (workbook.getWorksheets().getCount() <= 1) {
            return;
        }
        Worksheet first = workbook.getWorksheets().get(0);
        if (!"Sheet1".equals(first.getName())) {
            return;
        }
        if (first.getCells().getMaxDataRow() > -1 || first.getCells().getMaxDataColumn() > -1) {
            return;
        }
        workbook.getWorksheets().removeAt(0);
    }
}
