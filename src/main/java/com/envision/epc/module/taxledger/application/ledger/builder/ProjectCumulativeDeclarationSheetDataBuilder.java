package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.module.taxledger.application.dto.ProjectCumulativeDeclarationSheetDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.ProjectCumulativeDeclarationLedgerSheetData;
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
 * 项目累计申报 页数据构建器。
 */
@Component
@RequiredArgsConstructor
public class ProjectCumulativeDeclarationSheetDataBuilder implements LedgerSheetDataBuilder<ProjectCumulativeDeclarationLedgerSheetData> {
    private static final String HEADER_PERIOD = "所属期";
    private static final String HEADER_TOTAL = "税款合计";
    public static final String DECLARATION_TOTALS_KEY = "projectCumulativeDeclarationTotals";

    private final PreviousLedgerLocator previousLedgerLocator;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PROJECT_CUMULATIVE_DECLARATION;
    }

    @Override
    public ProjectCumulativeDeclarationLedgerSheetData build(LedgerBuildContext ctx) {
        String normalizedYm = PreviousLedgerLocator.normalizeYearMonth(ctx.getYearMonth());
        YearMonth yearMonth = YearMonth.parse(normalizedYm);
        int targetYear = yearMonth.getYear();
        int targetMonth = yearMonth.getMonthValue();

        ProjectCumulativeDeclarationSheetDTO currentParsed =
                ctx.getParsedObject(FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION, ProjectCumulativeDeclarationSheetDTO.class);
        ProjectCumulativeDeclarationSheetDTO previousParsed = parsePrevious(ctx, normalizedYm);

        List<String> taxHeaders = resolveTaxHeaders(ctx, previousParsed, currentParsed);
        Map<String, MutableRow> rowsByPeriod = initYearRows(targetYear);

        mergeRows(previousParsed, rowsByPeriod, taxHeaders, targetYear, null);
        mergeRows(currentParsed, rowsByPeriod, taxHeaders, targetYear, targetMonth);

        List<ProjectCumulativeDeclarationLedgerSheetData.RowData> monthRows = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            String key = periodKey(targetYear, month);
            MutableRow row = rowsByPeriod.get(key);
            monthRows.add(toRowData(row, false));
        }
        ProjectCumulativeDeclarationLedgerSheetData.RowData totalRow = buildTotalRow(targetYear, taxHeaders, rowsByPeriod);
        publishTotalsSnapshot(ctx, monthRows);
        return new ProjectCumulativeDeclarationLedgerSheetData(String.valueOf(targetYear), taxHeaders, monthRows, totalRow);
    }

    private void publishTotalsSnapshot(LedgerBuildContext ctx,
                                       List<ProjectCumulativeDeclarationLedgerSheetData.RowData> monthRows) {
        if (ctx.getPreloadSummary() == null) {
            return;
        }
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (ProjectCumulativeDeclarationLedgerSheetData.RowData row : monthRows) {
            if (row == null || row.getPeriodKey() == null) {
                continue;
            }
            totals.put(row.getPeriodKey(), row.getTaxTotal());
        }
        ctx.getPreloadSummary().put(DECLARATION_TOTALS_KEY, totals);
    }

    private Map<String, MutableRow> initYearRows(int year) {
        Map<String, MutableRow> rows = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            String key = periodKey(year, month);
            rows.put(key, new MutableRow(key, year + "年" + month + "月"));
        }
        return rows;
    }

    private ProjectCumulativeDeclarationSheetDTO parsePrevious(LedgerBuildContext ctx, String currentYearMonth) {
        PreviousLedgerLocator.PreviousLedgerRef previous = previousLedgerLocator.find(ctx.getCompanyCode(), currentYearMonth);
        if (previous == null || previous.ledgerBlobPath() == null || previous.ledgerBlobPath().isBlank()) {
            return null;
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(previous.ledgerBlobPath(), out);
            ParseResult<?> parsed = sheetParseService.parse(
                    new ByteArrayInputStream(out.toByteArray()),
                    FileCategoryEnum.PROJECT_CUMULATIVE_DECLARATION,
                    ParseContext.builder()
                            .companyCode(ctx.getCompanyCode())
                            .yearMonth(ctx.getYearMonth())
                            .fileName("previous-final-ledger")
                            .operator(ctx.getOperator())
                            .traceId(ctx.getTraceId())
                            .build()
            );
            if (parsed == null || parsed.hasError() || !(parsed.getData() instanceof ProjectCumulativeDeclarationSheetDTO)) {
                return null;
            }
            return (ProjectCumulativeDeclarationSheetDTO) parsed.getData();
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> resolveTaxHeaders(LedgerBuildContext ctx,
                                           ProjectCumulativeDeclarationSheetDTO previous,
                                           ProjectCumulativeDeclarationSheetDTO current) {
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        collectHeaders(headers, previous);
        collectHeaders(headers, current);

        List<TaxCategoryConfig> configList = ctx.getConfigSnapshot() == null
                ? List.of()
                : ctx.getConfigSnapshot().getTaxCategoryConfigs();
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

    private void collectHeaders(LinkedHashSet<String> collector, ProjectCumulativeDeclarationSheetDTO dto) {
        if (dto == null || dto.getRows() == null) {
            return;
        }
        for (List<ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO> row : dto.getRows()) {
            if (row == null) {
                continue;
            }
            for (ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO cell : row) {
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

    private void mergeRows(ProjectCumulativeDeclarationSheetDTO source,
                           Map<String, MutableRow> rowsByPeriod,
                           List<String> taxHeaders,
                           int targetYear,
                           Integer monthFilter) {
        if (source == null || source.getRows() == null || source.getRows().isEmpty()) {
            return;
        }
        for (List<ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO> row : source.getRows()) {
            if (row == null || row.isEmpty()) {
                continue;
            }
            String rowPeriod = normalizePeriod(row.get(0).getPeriod());
            if (rowPeriod == null) {
                continue;
            }
            YearMonth ym;
            try {
                ym = YearMonth.parse(toStandardPeriod(rowPeriod));
            } catch (Exception ex) {
                continue;
            }
            if (ym.getYear() != targetYear) {
                continue;
            }
            if (monthFilter != null && ym.getMonthValue() != monthFilter) {
                continue;
            }
            MutableRow targetRow = rowsByPeriod.get(periodKey(targetYear, ym.getMonthValue()));
            if (targetRow == null) {
                continue;
            }
            for (ProjectCumulativeDeclarationSheetDTO.ProjectTaxCellDTO cell : row) {
                if (cell == null) {
                    continue;
                }
                String header = normalizeHeader(cell.getHeaderName());
                if (header == null || header.isBlank() || !taxHeaders.contains(header)) {
                    continue;
                }
                targetRow.values.put(header, cell.getValue());
            }
        }
    }

    private ProjectCumulativeDeclarationLedgerSheetData.RowData toRowData(MutableRow mutable, boolean totalRow) {
        LinkedHashMap<String, BigDecimal> values = new LinkedHashMap<>(mutable.values);
        BigDecimal rowTotal = sumSkipNull(values.values());
        if (rowTotal == null) {
            rowTotal = BigDecimal.ZERO;
        }
        return new ProjectCumulativeDeclarationLedgerSheetData.RowData(
                mutable.periodKey, mutable.periodLabel, values, rowTotal, totalRow);
    }

    private ProjectCumulativeDeclarationLedgerSheetData.RowData buildTotalRow(int year,
                                                                               List<String> taxHeaders,
                                                                               Map<String, MutableRow> rowsByPeriod) {
        LinkedHashMap<String, BigDecimal> totals = new LinkedHashMap<>();
        for (String header : taxHeaders) {
            List<BigDecimal> values = new ArrayList<>(12);
            for (int month = 1; month <= 12; month++) {
                MutableRow row = rowsByPeriod.get(periodKey(year, month));
                values.add(row == null ? null : row.values.get(header));
            }
            BigDecimal sum = sumSkipNull(values);
            totals.put(header, sum == null ? BigDecimal.ZERO : sum);
        }
        BigDecimal grandTotal = sumSkipNull(totals.values());
        if (grandTotal == null) {
            grandTotal = BigDecimal.ZERO;
        }
        return new ProjectCumulativeDeclarationLedgerSheetData.RowData(
                periodKey(year, 0), "合计", totals, grandTotal, true);
    }

    private BigDecimal sumSkipNull(Iterable<BigDecimal> values) {
        BigDecimal sum = BigDecimal.ZERO;
        boolean hasValue = false;
        for (BigDecimal value : values) {
            if (value == null) {
                continue;
            }
            sum = sum.add(value);
            hasValue = true;
        }
        return hasValue ? sum : null;
    }

    private String normalizeHeader(String header) {
        String value = normalize(header);
        if (value.isBlank()) {
            return null;
        }
        if (HEADER_PERIOD.equals(value) || HEADER_TOTAL.equals(value)) {
            return null;
        }
        return value;
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\u00A0", " ").trim();
    }

    private String periodKey(int year, int month) {
        return month <= 0 ? year + "-0" : year + "-" + month;
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

    private String toStandardPeriod(String normalized) {
        String[] parts = normalized.split("-");
        int month = Integer.parseInt(parts[1]);
        return parts[0] + "-" + String.format("%02d", month);
    }

    private static class MutableRow {
        private final String periodKey;
        private final String periodLabel;
        private final Map<String, BigDecimal> values = new LinkedHashMap<>();

        private MutableRow(String periodKey, String periodLabel) {
            this.periodKey = periodKey;
            this.periodLabel = periodLabel;
        }
    }
}

