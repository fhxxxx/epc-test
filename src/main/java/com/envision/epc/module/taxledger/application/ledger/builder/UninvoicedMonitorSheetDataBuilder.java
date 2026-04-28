package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOutputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.UninvoicedMonitorItemDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.UninvoicedMonitorLedgerSheetData;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/**
 * 未开票数监控 页数据构建器。
 */
@Component
@RequiredArgsConstructor
public class UninvoicedMonitorSheetDataBuilder implements LedgerSheetDataBuilder<UninvoicedMonitorLedgerSheetData> {
    private static final String MAIN_REVENUE_ITEM = "主营业务收入";
    private static final String INTEREST_ACCOUNT = "6603020011";
    private static final String OTHER_INCOME_ACCOUNT = "6702000010";

    private final PreviousLedgerLocator previousLedgerLocator;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.UNINVOICED_MONITOR;
    }

    @Override
    public UninvoicedMonitorLedgerSheetData build(LedgerBuildContext ctx) {
        String normalizedYm = PreviousLedgerLocator.normalizeYearMonth(ctx.getYearMonth());
        YearMonth yearMonth = YearMonth.parse(normalizedYm);
        int currentMonth = yearMonth.getMonthValue();
        String ledgerYear = String.valueOf(yearMonth.getYear());

        Map<String, UninvoicedMonitorItemDTO> rowByPeriod = initBaseRows(ledgerYear);
        mergePreviousBase(ctx, normalizedYm, rowByPeriod);

        UninvoicedMonitorItemDTO current = buildCurrentMonthRow(ctx, ledgerYear, currentMonth);
        rowByPeriod.put(current.getPeriod(), current);

        List<UninvoicedMonitorItemDTO> rows = new ArrayList<>(13);
        for (int m = 1; m <= 12; m++) {
            rows.add(rowByPeriod.get(periodOf(ledgerYear, m)));
        }
        rows.add(buildTotalRow(rows));
        return new UninvoicedMonitorLedgerSheetData(ledgerYear, rows);
    }

    private Map<String, UninvoicedMonitorItemDTO> initBaseRows(String year) {
        Map<String, UninvoicedMonitorItemDTO> rows = new LinkedHashMap<>();
        for (int month = 1; month <= 12; month++) {
            UninvoicedMonitorItemDTO item = new UninvoicedMonitorItemDTO();
            item.setPeriod(periodOf(year, month));
            rows.put(item.getPeriod(), item);
        }
        return rows;
    }

    private void mergePreviousBase(LedgerBuildContext ctx, String currentYearMonth, Map<String, UninvoicedMonitorItemDTO> rowByPeriod) {
        PreviousLedgerLocator.PreviousLedgerRef previous = previousLedgerLocator.find(ctx.getCompanyCode(), currentYearMonth);
        if (previous == null || previous.ledgerBlobPath() == null || previous.ledgerBlobPath().isBlank()) {
            return;
        }
        List<UninvoicedMonitorItemDTO> previousRows = parsePreviousRows(ctx, previous.ledgerBlobPath());
        for (UninvoicedMonitorItemDTO previousRow : previousRows) {
            if (previousRow == null || previousRow.getPeriod() == null) {
                continue;
            }
            String normalizedPeriod = normalizePeriod(previousRow.getPeriod());
            if (normalizedPeriod == null || !rowByPeriod.containsKey(normalizedPeriod)) {
                continue;
            }
            UninvoicedMonitorItemDTO copy = copyRow(previousRow);
            copy.setPeriod(normalizedPeriod);
            rowByPeriod.put(normalizedPeriod, copy);
        }
    }

    private List<UninvoicedMonitorItemDTO> parsePreviousRows(LedgerBuildContext ctx, String blobPath) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(blobPath, out);
            ParseResult<?> parsed = sheetParseService.parse(
                    new ByteArrayInputStream(out.toByteArray()),
                    FileCategoryEnum.UNINVOICED_MONITOR,
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
            List<UninvoicedMonitorItemDTO> rows = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof UninvoicedMonitorItemDTO) {
                    rows.add((UninvoicedMonitorItemDTO) item);
                }
            }
            return rows;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private UninvoicedMonitorItemDTO buildCurrentMonthRow(LedgerBuildContext ctx, String year, int month) {
        List<PlStatementRowDTO> plRows = SheetDataReaders.requireList(ctx, FileCategoryEnum.PL, PlStatementRowDTO.class, support());
        DlOtherParsedDTO dlOther = SheetDataReaders.requireObject(ctx, FileCategoryEnum.DL_OTHER, DlOtherParsedDTO.class, support());
        DlOutputParsedDTO dlOutput = SheetDataReaders.requireObject(ctx, FileCategoryEnum.DL_OUTPUT, DlOutputParsedDTO.class, support());
        VatOutputSheetUploadDTO vatOutput = SheetDataReaders.requireObject(ctx, FileCategoryEnum.VAT_OUTPUT, VatOutputSheetUploadDTO.class, support());

        BigDecimal declaredMainBusinessRevenue = sumMainBusinessRevenue(plRows);
        BigDecimal declaredInterestIncome = accountDocumentAmount(dlOther, INTEREST_ACCOUNT);
        BigDecimal declaredOtherIncome = accountDocumentAmount(dlOther, OTHER_INCOME_ACCOUNT);
        BigDecimal declaredOutputTax = nvl(dlOutput == null ? null : dlOutput.getDocumentAmountSum()).negate();

        BigDecimal invoicedSalesIncome = sumVatOutputBlueAmount(vatOutput);
        BigDecimal invoicedOutputTax = sumVatOutputBlueTax(vatOutput);

        UninvoicedMonitorItemDTO row = new UninvoicedMonitorItemDTO();
        row.setPeriod(periodOf(year, month));
        row.setDeclaredMainBusinessRevenue(declaredMainBusinessRevenue);
        row.setDeclaredInterestIncome(declaredInterestIncome);
        row.setDeclaredOtherIncome(declaredOtherIncome);
        row.setDeclaredOutputTax(declaredOutputTax);
        row.setInvoicedSalesIncome(invoicedSalesIncome);
        row.setInvoicedOutputTax(invoicedOutputTax);
        row.setUninvoicedSalesIncome(declaredMainBusinessRevenue
                .add(declaredInterestIncome)
                .add(declaredOtherIncome)
                .subtract(invoicedSalesIncome));
        row.setUninvoicedOutputTax(declaredOutputTax.subtract(invoicedOutputTax));
        return row;
    }

    private BigDecimal sumMainBusinessRevenue(List<PlStatementRowDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "未开票数监控缺少PL数据");
        }
        BigDecimal total = BigDecimal.ZERO;
        for (PlStatementRowDTO row : rows) {
            if (row == null || row.getItemName() == null) {
                continue;
            }
            String itemName = row.getItemName().replace(" ", "").trim();
            if (itemName.contains(MAIN_REVENUE_ITEM)) {
                total = total.add(nvl(row.getCurrentPeriodAmount()));
            }
        }
        return total;
    }

    private BigDecimal accountDocumentAmount(DlOtherParsedDTO dto, String accountCode) {
        if (dto == null || dto.getDocumentAmountSumByAccount() == null) {
            return BigDecimal.ZERO;
        }
        return nvl(dto.getDocumentAmountSumByAccount().get(accountCode));
    }

    private BigDecimal sumVatOutputBlueAmount(VatOutputSheetUploadDTO dto) {
        VatOutputSheetUploadDTO.TaxRateSummaryItem total = findVatSummaryTotalRow(dto);
        if (total != null) {
            return nvl(total.getBlueInvoiceAmount());
        }
        BigDecimal sum = BigDecimal.ZERO;
        if (dto == null || dto.getTaxRateSummaries() == null) {
            return sum;
        }
        for (VatOutputSheetUploadDTO.TaxRateSummaryItem item : dto.getTaxRateSummaries()) {
            if (item == null) {
                continue;
            }
            sum = sum.add(nvl(item.getBlueInvoiceAmount()));
        }
        return sum;
    }

    private BigDecimal sumVatOutputBlueTax(VatOutputSheetUploadDTO dto) {
        VatOutputSheetUploadDTO.TaxRateSummaryItem total = findVatSummaryTotalRow(dto);
        if (total != null) {
            return nvl(total.getBlueInvoiceTaxAmount());
        }
        BigDecimal sum = BigDecimal.ZERO;
        if (dto == null || dto.getTaxRateSummaries() == null) {
            return sum;
        }
        for (VatOutputSheetUploadDTO.TaxRateSummaryItem item : dto.getTaxRateSummaries()) {
            if (item == null) {
                continue;
            }
            sum = sum.add(nvl(item.getBlueInvoiceTaxAmount()));
        }
        return sum;
    }

    private VatOutputSheetUploadDTO.TaxRateSummaryItem findVatSummaryTotalRow(VatOutputSheetUploadDTO dto) {
        if (dto == null || dto.getTaxRateSummaries() == null) {
            return null;
        }
        for (VatOutputSheetUploadDTO.TaxRateSummaryItem item : dto.getTaxRateSummaries()) {
            if (item == null) {
                continue;
            }
            if (containsTotal(item.getSerialNo()) || containsTotal(item.getInvoiceStatus())) {
                return item;
            }
        }
        return null;
    }

    private boolean containsTotal(String value) {
        return value != null && value.replace(" ", "").contains("合计");
    }

    private UninvoicedMonitorItemDTO buildTotalRow(List<UninvoicedMonitorItemDTO> monthRows) {
        UninvoicedMonitorItemDTO total = new UninvoicedMonitorItemDTO();
        total.setPeriod("合计数");
        BigDecimal declaredMain = BigDecimal.ZERO;
        BigDecimal declaredInterest = BigDecimal.ZERO;
        BigDecimal declaredOther = BigDecimal.ZERO;
        BigDecimal declaredOutputTax = BigDecimal.ZERO;
        BigDecimal invoicedSales = BigDecimal.ZERO;
        BigDecimal invoicedTax = BigDecimal.ZERO;
        BigDecimal uninvoicedSales = BigDecimal.ZERO;
        BigDecimal uninvoicedTax = BigDecimal.ZERO;
        for (UninvoicedMonitorItemDTO row : monthRows) {
            if (row == null) {
                continue;
            }
            declaredMain = declaredMain.add(nvl(row.getDeclaredMainBusinessRevenue()));
            declaredInterest = declaredInterest.add(nvl(row.getDeclaredInterestIncome()));
            declaredOther = declaredOther.add(nvl(row.getDeclaredOtherIncome()));
            declaredOutputTax = declaredOutputTax.add(nvl(row.getDeclaredOutputTax()));
            invoicedSales = invoicedSales.add(nvl(row.getInvoicedSalesIncome()));
            invoicedTax = invoicedTax.add(nvl(row.getInvoicedOutputTax()));
            uninvoicedSales = uninvoicedSales.add(nvl(row.getUninvoicedSalesIncome()));
            uninvoicedTax = uninvoicedTax.add(nvl(row.getUninvoicedOutputTax()));
        }
        total.setDeclaredMainBusinessRevenue(declaredMain);
        total.setDeclaredInterestIncome(declaredInterest);
        total.setDeclaredOtherIncome(declaredOther);
        total.setDeclaredOutputTax(declaredOutputTax);
        total.setInvoicedSalesIncome(invoicedSales);
        total.setInvoicedOutputTax(invoicedTax);
        total.setUninvoicedSalesIncome(uninvoicedSales);
        total.setUninvoicedOutputTax(uninvoicedTax);
        return total;
    }

    private UninvoicedMonitorItemDTO copyRow(UninvoicedMonitorItemDTO source) {
        UninvoicedMonitorItemDTO copy = new UninvoicedMonitorItemDTO();
        copy.setPeriod(source.getPeriod());
        copy.setDeclaredMainBusinessRevenue(source.getDeclaredMainBusinessRevenue());
        copy.setDeclaredInterestIncome(source.getDeclaredInterestIncome());
        copy.setDeclaredOtherIncome(source.getDeclaredOtherIncome());
        copy.setDeclaredOutputTax(source.getDeclaredOutputTax());
        copy.setInvoicedSalesIncome(source.getInvoicedSalesIncome());
        copy.setInvoicedOutputTax(source.getInvoicedOutputTax());
        copy.setUninvoicedSalesIncome(source.getUninvoicedSalesIncome());
        copy.setUninvoicedOutputTax(source.getUninvoicedOutputTax());
        return copy;
    }

    private String periodOf(String year, int month) {
        return year + "-" + month;
    }

    private String normalizePeriod(String period) {
        if (period == null) {
            return null;
        }
        String normalized = period.trim();
        if (normalized.matches("^\\d{4}-\\d{2}$")) {
            String[] parts = normalized.split("-");
            try {
                return parts[0] + "-" + Integer.parseInt(parts[1]);
            } catch (Exception ex) {
                return null;
            }
        }
        if (normalized.matches("^\\d{4}-\\d{1,2}$")) {
            return normalized;
        }
        return null;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

