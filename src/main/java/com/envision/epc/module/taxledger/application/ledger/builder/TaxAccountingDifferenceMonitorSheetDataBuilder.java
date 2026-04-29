package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.TaxAccountingDifferenceMonitor23202355ItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.TaxAccountingDifferenceMonitorLedgerSheetData;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SheetParseService;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 账税差异监控-2320、2355 页数据构建器。
 */
@Component
@RequiredArgsConstructor
public class TaxAccountingDifferenceMonitorSheetDataBuilder implements LedgerSheetDataBuilder<TaxAccountingDifferenceMonitorLedgerSheetData> {
    private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]");
    private static final String BASE_ITEM_PREFIX = "当月利润表主营业务收入";
    private static final String PREVIOUS_LEDGER_REQUIRED_MSG = "未检测到前序台账，请先上传初始台账后再生成。";

    private final PreviousLedgerLocator previousLedgerLocator;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR;
    }

    @Override
    public TaxAccountingDifferenceMonitorLedgerSheetData build(LedgerBuildContext ctx) {
        if (!isCompany2320Or2355(ctx.getCompanyCode())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "账税差异监控-2320、2355 仅适用于2320、2355公司");
        }
        String normalizedYm = PreviousLedgerLocator.normalizeYearMonth(ctx.getYearMonth());
        YearMonth current = YearMonth.parse(normalizedYm);
        String ledgerYear = String.valueOf(current.getYear());
        String currentPeriod = current.getYear() + String.format("%02d", current.getMonthValue());

        PreviousLedgerLocator.PreviousLedgerRef previous = previousLedgerLocator.find(ctx.getCompanyCode(), normalizedYm);
        if (previous == null || isBlank(previous.ledgerBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, PREVIOUS_LEDGER_REQUIRED_MSG);
        }

        List<TaxAccountingDifferenceMonitor23202355ItemDTO> historyRows = parsePreviousRows(ctx, previous.ledgerBlobPath());
        if (historyRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "前序台账缺少账税差异监控历史数据");
        }

        List<String> categoryTitles = extractCategoryTitles(historyRows);
        if (categoryTitles.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "前序台账账税差异监控缺少动态分类标题");
        }

        List<VatChangeRowDTO> vatChangeRows = SheetDataReaders.requireList(
                ctx, FileCategoryEnum.VAT_CHANGE, VatChangeRowDTO.class, LedgerSheetCode.TAX_ACCOUNTING_DIFFERENCE_MONITOR);
        TaxAccountingDifferenceMonitor23202355ItemDTO currentRow = buildCurrentRow(currentPeriod, categoryTitles, vatChangeRows);

        List<TaxAccountingDifferenceMonitor23202355ItemDTO> mergedRows = mergeRows(historyRows, currentRow);
        return new TaxAccountingDifferenceMonitorLedgerSheetData(ledgerYear, categoryTitles, mergedRows);
    }

    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }

    private List<TaxAccountingDifferenceMonitor23202355ItemDTO> parsePreviousRows(LedgerBuildContext ctx, String blobPath) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(blobPath, out);
            ParseResult<?> parsed = sheetParseService.parse(
                    new ByteArrayInputStream(out.toByteArray()),
                    FileCategoryEnum.TAX_ACCOUNTING_DIFFERENCE_MONITOR,
                    ParseContext.builder()
                            .companyCode(ctx.getCompanyCode())
                            .yearMonth(ctx.getYearMonth())
                            .fileName("previous-final-ledger")
                            .operator(ctx.getOperator())
                            .traceId(ctx.getTraceId())
                            .build()
            );
            if (parsed == null || parsed.hasError() || !(parsed.getData() instanceof List<?>)) {
                return List.of();
            }
            List<?> list = (List<?>) parsed.getData();
            List<TaxAccountingDifferenceMonitor23202355ItemDTO> rows = new ArrayList<>();
            for (Object obj : list) {
                if (obj instanceof TaxAccountingDifferenceMonitor23202355ItemDTO) {
                    rows.add((TaxAccountingDifferenceMonitor23202355ItemDTO) obj);
                }
            }
            return rows;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private List<String> extractCategoryTitles(List<TaxAccountingDifferenceMonitor23202355ItemDTO> rows) {
        for (TaxAccountingDifferenceMonitor23202355ItemDTO row : rows) {
            if (row == null || row.getCategoryIncomeList() == null || row.getCategoryIncomeList().isEmpty()) {
                continue;
            }
            List<String> titles = new ArrayList<>();
            for (TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem item : row.getCategoryIncomeList()) {
                if (item == null || isBlank(item.getTitle())) {
                    continue;
                }
                titles.add(item.getTitle().trim());
            }
            if (!titles.isEmpty()) {
                return titles;
            }
        }
        return List.of();
    }

    private TaxAccountingDifferenceMonitor23202355ItemDTO buildCurrentRow(String currentPeriod,
                                                                           List<String> categoryTitles,
                                                                           List<VatChangeRowDTO> vatChangeRows) {
        TaxAccountingDifferenceMonitor23202355ItemDTO row = new TaxAccountingDifferenceMonitor23202355ItemDTO();
        row.setPeriod(currentPeriod);
        List<TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem> categoryIncomeList = new ArrayList<>();

        BigDecimal totalBookIncome = BigDecimal.ZERO;
        BigDecimal totalDeclaredIncome = BigDecimal.ZERO;
        for (String title : categoryTitles) {
            TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem item =
                    new TaxAccountingDifferenceMonitor23202355ItemDTO.CategoryIncomeItem();
            item.setTitle(title);
            item.setBookIncome(null);
            item.setDeclaredIncome(sumDeclaredIncomeByTitle(vatChangeRows, title));
            categoryIncomeList.add(item);

            totalBookIncome = totalBookIncome.add(nvl(item.getBookIncome()));
            totalDeclaredIncome = totalDeclaredIncome.add(nvl(item.getDeclaredIncome()));
        }

        row.setCategoryIncomeList(categoryIncomeList);
        row.setTotalBookIncome(totalBookIncome);
        row.setTotalVatDeclaredIncome(totalDeclaredIncome);
        row.setAccountingTaxDifference(totalBookIncome.subtract(totalDeclaredIncome));
        row.setDifferenceAnalysis("");
        return row;
    }

    private BigDecimal sumDeclaredIncomeByTitle(List<VatChangeRowDTO> rows, String title) {
        if (rows == null || rows.isEmpty() || isBlank(title)) {
            return BigDecimal.ZERO;
        }
        String titleRate = extractRateToken(title);
        if (titleRate == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (VatChangeRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (!baseItemMatched(row.getBaseItem())) {
                continue;
            }
            String splitRate = extractRateToken(row.getSplitBasis());
            if (splitRate == null || !splitRate.equals(titleRate)) {
                continue;
            }
            sum = sum.add(nvl(row.getTotalAmount()));
        }
        return sum;
    }

    private boolean baseItemMatched(String baseItem) {
        if (isBlank(baseItem)) {
            return false;
        }
        String normalized = baseItem.replace(" ", "").trim();
        return normalized.startsWith(BASE_ITEM_PREFIX);
    }

    private String extractRateToken(String text) {
        if (isBlank(text)) {
            return null;
        }
        Matcher matcher = RATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1);
    }

    private List<TaxAccountingDifferenceMonitor23202355ItemDTO> mergeRows(
            List<TaxAccountingDifferenceMonitor23202355ItemDTO> historyRows,
            TaxAccountingDifferenceMonitor23202355ItemDTO currentRow) {
        Map<String, TaxAccountingDifferenceMonitor23202355ItemDTO> map = new LinkedHashMap<>();
        for (TaxAccountingDifferenceMonitor23202355ItemDTO row : historyRows) {
            if (row == null || isBlank(row.getPeriod())) {
                continue;
            }
            map.put(row.getPeriod(), row);
        }
        map.put(currentRow.getPeriod(), currentRow);

        List<TaxAccountingDifferenceMonitor23202355ItemDTO> merged = new ArrayList<>(map.values());
        merged.sort(Comparator.comparing(this::periodSortKey));
        return merged;
    }

    private int periodSortKey(TaxAccountingDifferenceMonitor23202355ItemDTO row) {
        if (row == null || isBlank(row.getPeriod())) {
            return Integer.MAX_VALUE;
        }
        String period = row.getPeriod().trim();
        if (!period.matches("\\d{6}")) {
            return Integer.MAX_VALUE;
        }
        try {
            return Integer.parseInt(period);
        } catch (Exception ex) {
            return Integer.MAX_VALUE;
        }
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }
}

