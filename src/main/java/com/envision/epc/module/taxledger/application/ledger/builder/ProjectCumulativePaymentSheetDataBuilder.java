package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativePaymentSheetDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativeDeclarationLedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativePaymentLedgerSheetData;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SheetParseService;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.TaxCategoryConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


/**
 * 项目累计缴纳 页数据构建器。
 */
@Component
@RequiredArgsConstructor
public class ProjectCumulativePaymentSheetDataBuilder implements LedgerSheetDataBuilder<ProjectCumulativePaymentLedgerSheetData> {
    private static final String HEADER_PERIOD_1 = "实际缴纳所属期";
    private static final String HEADER_PERIOD_2 = "所属期";
    private static final String HEADER_PAID = "实缴税金";
    private static final String HEADER_DECLARED = "申请税金";
    private static final String HEADER_DIFF = "差异";
    private static final String HEADER_REASON = "原因";

    private final PreviousLedgerLocator previousLedgerLocator;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_PAYMENT;
    }

    @Override
    public ProjectCumulativePaymentLedgerSheetData build(LedgerBuildContext ctx) {
        String normalizedYm = PreviousLedgerLocator.normalizeYearMonth(ctx.getYearMonth());
        YearMonth ym = YearMonth.parse(normalizedYm);
        int targetYear = ym.getYear();
        int targetMonth = ym.getMonthValue();

        ProjectCumulativePaymentSheetDTO currentParsed =
                ctx.getParsedObject(FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT, ProjectCumulativePaymentSheetDTO.class);
        ProjectCumulativePaymentSheetDTO previousParsed = parsePrevious(ctx, normalizedYm);

        List<String> taxHeaders = resolveTaxHeaders(ctx, previousParsed, currentParsed);
        Map<String, MutableRow> rowByPeriod = initYearRows(targetYear);

        mergeRows(previousParsed, rowByPeriod, taxHeaders, targetYear, null);
        mergeRows(currentParsed, rowByPeriod, taxHeaders, targetYear, targetMonth);

        Map<String, BigDecimal> declarationTotals = resolveDeclarationTotals(ctx);
        applyDeclarationTotals(rowByPeriod, declarationTotals, targetYear);
        clearCurrentMonthTaxValues(rowByPeriod, taxHeaders, targetYear, targetMonth);

        List<ProjectCumulativePaymentLedgerSheetData.RowData> monthRows = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            MutableRow row = rowByPeriod.get(periodKey(targetYear, month));
            monthRows.add(toRowData(row, false));
        }
        ProjectCumulativePaymentLedgerSheetData.RowData totalRow =
                new ProjectCumulativePaymentLedgerSheetData.RowData(periodKey(targetYear, 0), "合计",
                        initEmptyHeaderMap(taxHeaders), null, "", true);
        return new ProjectCumulativePaymentLedgerSheetData(String.valueOf(targetYear), taxHeaders, monthRows, totalRow);
    }

    private Map<String, MutableRow> initYearRows(int year) {
        Map<String, MutableRow> rows = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            String key = periodKey(year, month);
            rows.put(key, new MutableRow(key, year + "年" + month + "月"));
        }
        return rows;
    }

    private ProjectCumulativePaymentSheetDTO parsePrevious(LedgerBuildContext ctx, String currentYearMonth) {
        PreviousLedgerLocator.PreviousLedgerRef previous = previousLedgerLocator.find(ctx.getCompanyCode(), currentYearMonth);
        if (previous == null || previous.ledgerBlobPath() == null || previous.ledgerBlobPath().isBlank()) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(previous.ledgerBlobPath(), out);
            ParseResult<?> parsed = sheetParseService.parse(
                    new ByteArrayInputStream(out.toByteArray()),
                    FileCategoryEnum.PROJECT_CUMULATIVE_PAYMENT,
                    ParseContext.builder()
                            .companyCode(ctx.getCompanyCode())
                            .yearMonth(ctx.getYearMonth())
                            .fileName("previous-final-ledger")
                            .operator(ctx.getOperator())
                            .traceId(ctx.getTraceId())
                            .build()
            );
            if (parsed == null || parsed.hasError() || !(parsed.getData() instanceof ProjectCumulativePaymentSheetDTO)) {
                return null;
            }
            return (ProjectCumulativePaymentSheetDTO) parsed.getData();
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> resolveTaxHeaders(LedgerBuildContext ctx,
                                           ProjectCumulativePaymentSheetDTO previous,
                                           ProjectCumulativePaymentSheetDTO current) {
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        collectHeaders(headers, previous);
        collectHeaders(headers, current);

        List<TaxCategoryConfig> configList = ctx.getConfigSnapshot() == null ? List.of() : ctx.getConfigSnapshot().getTaxCategoryConfigs();
        if (configList != null && !configList.isEmpty()) {
            configList.stream()
                    .filter(cfg -> cfg != null && isCompanyConfigMatched(ctx.getCompanyCode(), cfg.getCompanyCode()))
                    .sorted(Comparator.comparing(this::seqSortKey).thenComparing(cfg -> normalize(cfg.getTaxType())))
                    .map(TaxCategoryConfig::getTaxType)
                    .map(this::normalizeHeader)
                    .filter(v -> v != null && !v.isBlank())
                    .forEach(headers::add);
        }
        return new ArrayList<>(headers);
    }

    private void collectHeaders(LinkedHashSet<String> collector, ProjectCumulativePaymentSheetDTO dto) {
        if (dto == null || dto.getRows() == null) {
            return;
        }
        for (ProjectCumulativePaymentSheetDTO.ProjectCumulativePaymentRowDTO row : dto.getRows()) {
            if (row == null || row.getDynamicTaxCells() == null) {
                continue;
            }
            for (ProjectCumulativePaymentSheetDTO.ProjectTaxCellDTO cell : row.getDynamicTaxCells()) {
                if (cell == null) {
                    continue;
                }
                String header = normalizeHeader(cell.getHeaderName());
                if (header != null && !header.isBlank()) {
                    collector.add(header);
                }
            }
        }
    }

    private boolean isCompanyConfigMatched(String companyCode, String cfgCompanyCode) {
        String cfg = normalize(cfgCompanyCode);
        if (cfg.isEmpty()) {
            return true;
        }
        return cfg.equals(normalize(companyCode));
    }

    private BigDecimal seqSortKey(TaxCategoryConfig cfg) {
        if (cfg == null || cfg.getSeqNo() == null || cfg.getSeqNo().isBlank()) {
            return new BigDecimal("99999");
        }
        try {
            return new BigDecimal(cfg.getSeqNo().trim());
        } catch (Exception ex) {
            return new BigDecimal("99999");
        }
    }

    private void mergeRows(ProjectCumulativePaymentSheetDTO source,
                           Map<String, MutableRow> rowByPeriod,
                           List<String> taxHeaders,
                           int targetYear,
                           Integer monthFilter) {
        if (source == null || source.getRows() == null || source.getRows().isEmpty()) {
            return;
        }
        for (ProjectCumulativePaymentSheetDTO.ProjectCumulativePaymentRowDTO sourceRow : source.getRows()) {
            if (sourceRow == null) {
                continue;
            }
            String normalizedPeriod = normalizePeriod(sourceRow.getPeriod());
            if (normalizedPeriod == null) {
                continue;
            }
            YearMonth ym;
            try {
                ym = YearMonth.parse(toStandardPeriod(normalizedPeriod));
            } catch (Exception ex) {
                continue;
            }
            if (ym.getYear() != targetYear) {
                continue;
            }
            if (monthFilter != null && ym.getMonthValue() != monthFilter) {
                continue;
            }
            MutableRow target = rowByPeriod.get(periodKey(targetYear, ym.getMonthValue()));
            if (target == null) {
                continue;
            }
            if (sourceRow.getDynamicTaxCells() != null) {
                for (ProjectCumulativePaymentSheetDTO.ProjectTaxCellDTO cell : sourceRow.getDynamicTaxCells()) {
                    if (cell == null) {
                        continue;
                    }
                    String header = normalizeHeader(cell.getHeaderName());
                    if (header == null || header.isBlank() || !taxHeaders.contains(header)) {
                        continue;
                    }
                    target.values.put(header, cell.getValue());
                }
            }
            target.reason = sourceRow.getReason();
        }
    }

    private Map<String, BigDecimal> resolveDeclarationTotals(LedgerBuildContext ctx) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        ProjectCumulativeDeclarationLedgerSheetData declarationData = ctx.requireBuilt(
                LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION,
                ProjectCumulativeDeclarationLedgerSheetData.class,
                support());
        if (declarationData.getMonthRows() == null) {
            return result;
        }
        for (ProjectCumulativeDeclarationLedgerSheetData.RowData row : declarationData.getMonthRows()) {
            if (row == null || row.getPeriodKey() == null) {
                continue;
            }
            String key = normalize(row.getPeriodKey());
            BigDecimal amount = row.getTaxTotal();
            result.put(key, amount);
        }
        return result;
    }

    private void applyDeclarationTotals(Map<String, MutableRow> rowByPeriod,
                                        Map<String, BigDecimal> declarationTotals,
                                        int targetYear) {
        for (int month = 1; month <= 12; month++) {
            String key = periodKey(targetYear, month);
            MutableRow row = rowByPeriod.get(key);
            if (row == null) {
                continue;
            }
            row.declaredTotal = declarationTotals.get(key);
        }
    }

    private void clearCurrentMonthTaxValues(Map<String, MutableRow> rowByPeriod,
                                            List<String> taxHeaders,
                                            int targetYear,
                                            int targetMonth) {
        MutableRow row = rowByPeriod.get(periodKey(targetYear, targetMonth));
        if (row == null) {
            return;
        }
        for (String header : taxHeaders) {
            row.values.put(header, null);
        }
    }

    private Map<String, BigDecimal> initEmptyHeaderMap(List<String> taxHeaders) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        for (String header : taxHeaders) {
            values.put(header, null);
        }
        return values;
    }

    private ProjectCumulativePaymentLedgerSheetData.RowData toRowData(MutableRow row, boolean totalRow) {
        LinkedHashMap<String, BigDecimal> values = new LinkedHashMap<>(row.values);
        return new ProjectCumulativePaymentLedgerSheetData.RowData(
                row.periodKey, row.periodLabel, values, row.declaredTotal, row.reason == null ? "" : row.reason, totalRow);
    }

    private String normalizeHeader(String header) {
        String value = normalize(header);
        if (value.isBlank()) {
            return null;
        }
        if (HEADER_PERIOD_1.equals(value)
                || HEADER_PERIOD_2.equals(value)
                || HEADER_PAID.equals(value)
                || HEADER_DECLARED.equals(value)
                || HEADER_DIFF.equals(value)
                || HEADER_REASON.equals(value)) {
            return null;
        }
        return value;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\u00A0", " ").trim();
    }

    private String normalizePeriod(String period) {
        if (period == null || period.isBlank()) {
            return null;
        }
        String text = period.trim();
        text = text.replace("年", "-").replace("月", "").replace("/", "-").replace(".", "-");
        if (text.matches("^\\d{4}-\\d{1,2}$")) {
            return text;
        }
        if (text.matches("^\\d{4}-\\d{2}$")) {
            return text.substring(0, 5) + String.valueOf(Integer.parseInt(text.substring(5)));
        }
        return null;
    }

    private String toStandardPeriod(String normalizedPeriod) {
        String[] parts = normalizedPeriod.split("-");
        int month = Integer.parseInt(parts[1]);
        return parts[0] + "-" + String.format("%02d", month);
    }

    private String periodKey(int year, int month) {
        return month <= 0 ? year + "-0" : year + "-" + month;
    }

    private static class MutableRow {
        private final String periodKey;
        private final String periodLabel;
        private final Map<String, BigDecimal> values = new LinkedHashMap<>();
        private BigDecimal declaredTotal;
        private String reason;

        private MutableRow(String periodKey, String periodLabel) {
            this.periodKey = periodKey;
            this.periodLabel = periodLabel;
        }
    }
}

