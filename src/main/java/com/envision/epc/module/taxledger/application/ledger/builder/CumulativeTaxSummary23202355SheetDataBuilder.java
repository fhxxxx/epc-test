package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.CumulativeTaxSummary23202355ColumnDTO;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.CumulativeTaxSummary23202355LedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.data.SummaryLedgerSheetData;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 累计税金汇总表-2320、2355 页数据构建器。
 */
@Component
@RequiredArgsConstructor
public class CumulativeTaxSummary23202355SheetDataBuilder implements LedgerSheetDataBuilder<CumulativeTaxSummary23202355LedgerSheetData> {
    private static final String OTHER_INCOME_ACCOUNT = "6702000010";
    private static final String INTEREST_INCOME_ACCOUNT = "6603020011";

    private final PreviousLedgerLocator previousLedgerLocator;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.CUMULATIVE_TAX_SUMMARY_2320_2355;
    }

    @Override
    public CumulativeTaxSummary23202355LedgerSheetData build(LedgerBuildContext ctx) {
        String normalizedYm = PreviousLedgerLocator.normalizeYearMonth(ctx.getYearMonth());
        String currentPeriod = normalizedYm.replace("-", "");
        YearMonth currentYearMonth = YearMonth.parse(normalizedYm);

        List<CumulativeTaxSummary23202355ColumnDTO> columns = new ArrayList<>(parsePreviousColumns(ctx, normalizedYm));
        columns.sort(Comparator.comparing(this::periodSortableValue));

        validatePreviousContinuity(columns, currentYearMonth);
        ensureCurrentPeriodNotExists(columns, currentPeriod);

        CumulativeTaxSummary23202355ColumnDTO current = buildCurrentPeriodColumn(ctx, currentPeriod, currentYearMonth);
        columns.add(current);
        return new CumulativeTaxSummary23202355LedgerSheetData(columns);
    }

    private List<CumulativeTaxSummary23202355ColumnDTO> parsePreviousColumns(LedgerBuildContext ctx, String currentYearMonth) {
        PreviousLedgerLocator.PreviousLedgerRef previous = previousLedgerLocator.find(ctx.getCompanyCode(), currentYearMonth);
        if (previous == null || previous.ledgerBlobPath() == null || previous.ledgerBlobPath().isBlank()) {
            return List.of();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(previous.ledgerBlobPath(), out);
            ParseResult<?> parsed = sheetParseService.parse(
                    new ByteArrayInputStream(out.toByteArray()),
                    FileCategoryEnum.CUMULATIVE_TAX_SUMMARY_2320_2355,
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
            List<?> rows = (List<?>) parsed.getData();
            List<CumulativeTaxSummary23202355ColumnDTO> result = new ArrayList<>();
            for (Object row : rows) {
                if (!(row instanceof CumulativeTaxSummary23202355ColumnDTO)) {
                    continue;
                }
                CumulativeTaxSummary23202355ColumnDTO dto = (CumulativeTaxSummary23202355ColumnDTO) row;
                if (dto.getPeriod() != null && !dto.getPeriod().isBlank()) {
                    result.add(dto);
                }
            }
            return result;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void validatePreviousContinuity(List<CumulativeTaxSummary23202355ColumnDTO> columns, YearMonth currentYearMonth) {
        if (columns.isEmpty()) {
            return;
        }
        CumulativeTaxSummary23202355ColumnDTO last = columns.get(columns.size() - 1);
        String previousExpected = currentYearMonth.minusMonths(1).toString().replace("-", "");
        String lastPeriod = normalizePeriod(last.getPeriod());
        if (!Objects.equals(lastPeriod, previousExpected)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "累计税金汇总表前序期间不连续，期望上期=" + previousExpected + "，实际=" + lastPeriod);
        }
    }

    private void ensureCurrentPeriodNotExists(List<CumulativeTaxSummary23202355ColumnDTO> columns, String currentPeriod) {
        for (CumulativeTaxSummary23202355ColumnDTO column : columns) {
            if (Objects.equals(normalizePeriod(column.getPeriod()), currentPeriod)) {
                throw new BizException(ErrorCode.BAD_REQUEST, "累计税金汇总表已存在当前期间列: " + currentPeriod);
            }
        }
    }

    private CumulativeTaxSummary23202355ColumnDTO buildCurrentPeriodColumn(LedgerBuildContext ctx,
                                                                           String currentPeriod,
                                                                           YearMonth currentYearMonth) {
        List<VatChangeRowDTO> vatRows = SheetDataReaders.requireList(
                ctx, FileCategoryEnum.VAT_CHANGE, VatChangeRowDTO.class, support());
        DlOtherParsedDTO dlOther = SheetDataReaders.requireObject(
                ctx, FileCategoryEnum.DL_OTHER, DlOtherParsedDTO.class, support());
        SummarySheetDTO summary = resolveBuiltSummary(ctx);

        BigDecimal vatPayable = sumByBaseItem(vatRows, "本期应交增值税", true);
        BigDecimal vatAmount = vatPayable;

        CumulativeTaxSummary23202355ColumnDTO dto = new CumulativeTaxSummary23202355ColumnDTO();
        dto.setPeriod(currentPeriod);
        dto.setBookIncome(sumByBaseItem(vatRows, "当月利润表主营业务收入", true));
        dto.setOtherIncome(sumDlLocalByAccount(dlOther, OTHER_INCOME_ACCOUNT));
        dto.setProjectInvoicing(sumByBaseItem(vatRows, "当月利润表主营业务收入", false));
        dto.setInterestInvoicing(sumDlLocalByAccount(dlOther, INTEREST_INCOME_ACCOUNT));
        dto.setOutputTaxA(sumByBaseItem(vatRows, "应交增值税-销项税", true));
        dto.setCurrentInputTaxB(sumByBaseItem(vatRows, "增值税已认证进项税", true));
        dto.setOpeningRetainedInputTaxC(sumByBaseItem(vatRows, "期初留抵进项税", true));
        dto.setInputTaxTransferOutD(sumByBaseItem(vatRows, "进项转出", true));
        dto.setRemotePrepaidVatE(sumByBaseItem(vatRows, "异地预缴递减", true));
        dto.setVatPayableAminusBminusCplusDminusE(vatPayable);
        dto.setVatAmount(vatAmount);

        dto.setStampDuty(readTax(summary, "印花税"));
        dto.setPropertyTax(readTax(summary, "房产税"));
        dto.setUrbanLandUseTax(readTax(summary, "城镇土地"));
        dto.setIndividualIncomeTax(readTax(summary, "个税"));
        dto.setDisabledPersonsEmploymentSecurityFund(readTax(summary, "残疾人"));
        dto.setCorporateIncomeTax(readCorporateIncomeTax(summary, currentYearMonth));

        BigDecimal urbanRate = resolveTaxRate(ctx, "城建税");
        BigDecimal eduRate = resolveTaxRate(ctx, "教育费附加");
        BigDecimal localEduRate = resolveTaxRate(ctx, "地方教育费附加");

        dto.setUrbanConstructionTax(vatAmount.multiply(urbanRate));
        dto.setEducationSurcharge(vatAmount.multiply(eduRate));
        dto.setLocalEducationSurcharge(vatAmount.multiply(localEduRate));
        dto.setTotalTaxAmount(sumTaxFields(dto));
        return dto;
    }

    private BigDecimal sumTaxFields(CumulativeTaxSummary23202355ColumnDTO dto) {
        return nvl(dto.getVatAmount())
                .add(nvl(dto.getStampDuty()))
                .add(nvl(dto.getUrbanConstructionTax()))
                .add(nvl(dto.getEducationSurcharge()))
                .add(nvl(dto.getLocalEducationSurcharge()))
                .add(nvl(dto.getPropertyTax()))
                .add(nvl(dto.getUrbanLandUseTax()))
                .add(nvl(dto.getCorporateIncomeTax()))
                .add(nvl(dto.getIndividualIncomeTax()))
                .add(nvl(dto.getDisabledPersonsEmploymentSecurityFund()));
    }

    private BigDecimal sumByBaseItem(List<VatChangeRowDTO> rows, String baseItem, boolean totalAmount) {
        BigDecimal sum = BigDecimal.ZERO;
        String normalizedBase = normalizeBase(baseItem);
        for (VatChangeRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (!Objects.equals(normalizeBase(row.getBaseItem()), normalizedBase)) {
                continue;
            }
            BigDecimal value = totalAmount ? row.getTotalAmount() : row.getCurrentMonthInvoicedAmount();
            sum = sum.add(nvl(value));
        }
        return sum;
    }

    private BigDecimal sumDlLocalByAccount(DlOtherParsedDTO dto, String accountCode) {
        if (dto == null || dto.getLocalAmountSumByAccount() == null) {
            return BigDecimal.ZERO;
        }
        return nvl(dto.getLocalAmountSumByAccount().get(accountCode));
    }

    private SummarySheetDTO resolveBuiltSummary(LedgerBuildContext ctx) {
        SummaryLedgerSheetData summaryData =
                ctx.requireBuilt(LedgerSheetCode.SUMMARY, SummaryLedgerSheetData.class, support());
        if (summaryData.getSummary() == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "缺少前置 builder 产物内容: producer=SUMMARY, consumer=" + support().name());
        }
        return summaryData.getSummary();
    }

    private BigDecimal readTax(SummarySheetDTO summary, String keyword) {
        BigDecimal sum = BigDecimal.ZERO;
        String normalizedKeyword = normalizeKey(keyword);
        if (summary.getStampDutyRows() != null) {
            for (SummarySheetDTO.StampDutyItem row : summary.getStampDutyRows()) {
                if (row == null) {
                    continue;
                }
                if (containsKeyword(row.getTaxType(), normalizedKeyword)) {
                    sum = sum.add(nvl(row.getActualTaxPayable()));
                }
            }
        }
        if (summary.getCommonTaxRows() != null) {
            for (SummarySheetDTO.CommonTaxItem row : summary.getCommonTaxRows()) {
                if (row == null) {
                    continue;
                }
                if (containsKeyword(row.getTaxType(), normalizedKeyword)) {
                    sum = sum.add(nvl(row.getActualTaxPayable()));
                }
            }
        }
        return sum;
    }

    private BigDecimal readCorporateIncomeTax(SummarySheetDTO summary, YearMonth currentYearMonth) {
        BigDecimal q1 = BigDecimal.ZERO;
        BigDecimal q2 = BigDecimal.ZERO;
        BigDecimal q3 = BigDecimal.ZERO;
        BigDecimal q4 = BigDecimal.ZERO;
        if (summary.getCorporateIncomeTaxRows() != null) {
            for (SummarySheetDTO.CorporateIncomeTaxItem row : summary.getCorporateIncomeTaxRows()) {
                if (row == null) {
                    continue;
                }
                q1 = q1.add(nvl(row.getQ1Tax()));
                q2 = q2.add(nvl(row.getQ2Tax()));
                q3 = q3.add(nvl(row.getQ3Tax()));
                q4 = q4.add(nvl(row.getQ4Tax()));
            }
        }
        int quarter = (currentYearMonth.getMonthValue() - 1) / 3 + 1;
        switch (quarter) {
            case 1:
                return q1;
            case 2:
                return q2;
            case 3:
                return q3;
            case 4:
                return q4;
            default:
                return BigDecimal.ZERO;
        }
    }

    private boolean containsKeyword(String text, String normalizedKeyword) {
        String normalized = normalizeKey(text);
        return normalized.contains(normalizedKeyword);
    }

    private BigDecimal resolveTaxRate(LedgerBuildContext ctx, String taxTypeKeyword) {
        if (ctx.getConfigSnapshot() == null || ctx.getConfigSnapshot().getTaxCategoryConfigs() == null) {
            return BigDecimal.ZERO;
        }
        String keyword = normalizeKey(taxTypeKeyword);
        for (TaxCategoryConfig config : ctx.getConfigSnapshot().getTaxCategoryConfigs()) {
            if (config == null) {
                continue;
            }
            String taxType = normalizeKey(config.getTaxType());
            if (taxType.contains(keyword)) {
                return nvl(config.getTaxRate());
            }
        }
        return BigDecimal.ZERO;
    }

    private int periodSortableValue(CumulativeTaxSummary23202355ColumnDTO dto) {
        String period = normalizePeriod(dto == null ? null : dto.getPeriod());
        if (period == null || !period.matches("^\\d{6}$")) {
            return Integer.MAX_VALUE;
        }
        return Integer.parseInt(period);
    }

    private String normalizePeriod(String period) {
        if (period == null) {
            return null;
        }
        String text = period.trim();
        if (text.matches("^\\d{6}$")) {
            return text;
        }
        if (text.matches("^\\d{4}-\\d{2}$")) {
            return text.replace("-", "");
        }
        return null;
    }

    private String normalizeBase(String baseItem) {
        if (baseItem == null) {
            return "";
        }
        return baseItem.replace("*", "")
                .replace("＊", "")
                .replace(" ", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeKey(String key) {
        if (key == null) {
            return "";
        }
        return key.replace(" ", "")
                .replace("*", "")
                .replace("＊", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
