package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import com.envision.epc.module.taxledger.application.dto.DlInputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.SummaryQuarterSnapshot;
import com.envision.epc.module.taxledger.application.ledger.data.SummaryLedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.ProjectConfig;
import com.envision.epc.module.taxledger.domain.TaxCategoryConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
public class SummaryLedgerSheetDataBuilder implements LedgerSheetDataBuilder<SummaryLedgerSheetData> {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final String DEFAULT_VARIANCE_REASON = "待核对";
    private static final Pattern SEQ_NO_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");
    private static final Pattern RATE_TOKEN_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final String VAT_PAYABLE_ITEM = "应交增值税";
    private static final String VAT_SUBTOTAL_FALLBACK_ISSUE = "增值税合计未命中VAT_CHANGE条目=应交增值税，按0处理";
    private static final String VAT_MAIN_REVENUE_PREFIX = "销项税额-主营业务收入";
    private static final String VAT_CHANGE_MAIN_REVENUE_PREFIX = "当月利润表主营业务收入-";

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.SUMMARY;
    }

    @Override
    public SummaryLedgerSheetData build(LedgerBuildContext ctx) {
        SummarySheetDTO dto = new SummarySheetDTO();
        dto.setCompanyName(resolveCompanyName(ctx));
        dto.setLedgerPeriod(normalizeYearMonth(ctx.getYearMonth()));

        List<String> issues = new ArrayList<>();
        List<TaxCategoryConfig> taxConfigs = resolveTaxConfigs(ctx);
        Map<String, StampAgg> stampAggMap = resolveStampAggMap(ctx, issues);
        VatBaseLookup vatBaseLookup = resolveVatBaseLookup(ctx);
        Map<String, BigDecimal> bookAmountByAccount = resolveBookAmountByAccount(ctx);
        Map<String, ProjectConfig> projectConfigByTaxCategory = resolveProjectConfigByTaxCategory(ctx);

        List<SummarySheetDTO.StampDutyItem> stampRows = new ArrayList<>();
        List<SummarySheetDTO.CommonTaxItem> vatRows = new ArrayList<>();
        List<SummarySheetDTO.CommonTaxItem> commonRows = new ArrayList<>();
        List<SummarySheetDTO.CorporateIncomeTaxItem> citRows = new ArrayList<>();
        BigDecimal vatSubtotalDeclared = null;
        boolean vatSubtotalResolved = false;

        int displaySeq = 0;
        for (TaxCategoryConfig config : taxConfigs) {
            if (config == null) {
                continue;
            }
            String taxType = safeText(config.getTaxType());
            String taxItem = safeText(config.getTaxCategory());
            String normalizedTaxType = normalizeText(taxType);
            boolean isIntegerLine = isIntegerSeq(config.getSeqNo());
            Integer rowSeqNo = null;
            if (isIntegerLine) {
                displaySeq++;
                rowSeqNo = displaySeq;
            }

            if (normalizedTaxType.contains("印花税")) {
                stampRows.add(buildStampRow(config, taxType, taxItem, rowSeqNo, stampAggMap, issues, ctx));
                continue;
            }
            if (normalizedTaxType.contains("企业所得税")) {
                citRows.add(buildCitRow(config, taxType, taxItem, projectConfigByTaxCategory));
                continue;
            }
            BigDecimal actualOverride = null;
            if (isVatTaxType(taxType) && isSubtotalLine(taxType, taxItem)) {
                if (!vatSubtotalResolved) {
                    vatSubtotalDeclared = resolveVatSubtotalDeclaredAmount(ctx, issues);
                    vatSubtotalResolved = true;
                }
                actualOverride = vatSubtotalDeclared;
            }
            SummarySheetDTO.CommonTaxItem row = buildCommonRow(
                    config, taxType, taxItem, rowSeqNo, vatBaseLookup, bookAmountByAccount, actualOverride, issues);
            if (isVatTaxType(taxType)) {
                vatRows.add(row);
            } else {
                commonRows.add(row);
            }
        }

        dto.setStampDutyRows(stampRows);
        dto.setVatTaxRows(vatRows);
        dto.setCommonTaxRows(commonRows);
        dto.setCorporateIncomeTaxRows(citRows);

        SummarySheetDTO.FinalTotalItem finalTotal = new SummarySheetDTO.FinalTotalItem();
        finalTotal.setTotalTitle(dto.getLedgerPeriod() + "应申报合计");
        dto.setFinalTotal(finalTotal);

        appendBuildIssues(ctx, issues);
        return new SummaryLedgerSheetData(dto);
    }

    private SummarySheetDTO.StampDutyItem buildStampRow(TaxCategoryConfig config,
                                                        String taxType,
                                                        String taxItem,
                                                        Integer rowSeqNo,
                                                        Map<String, StampAgg> stampAggMap,
                                                        List<String> issues,
                                                        LedgerBuildContext ctx) {
        SummarySheetDTO.StampDutyItem row = new SummarySheetDTO.StampDutyItem();
        row.setSeqNo(rowSeqNo);
        row.setTaxType(taxType);
        row.setTaxItem(taxItem);
        row.setTaxBasisDesc(safeText(config.getTaxBasis()));

        StampAgg agg = stampAggMap.get(normalizeText(taxItem));
        BigDecimal taxBase = zeroIfNull(agg == null ? null : agg.taxableAmount);
        BigDecimal levyRatio = normalizeRatio(config.getCollectionRatio());
        BigDecimal taxRate = config.getTaxRate() != null ? config.getTaxRate() : zeroIfNull(agg == null ? null : agg.taxRate);
        BigDecimal actual = taxBase.multiply(levyRatio).multiply(taxRate).setScale(2, RoundingMode.HALF_UP);

        if (agg == null && taxItem != null && !taxItem.isBlank()) {
            issues.add("印花税未命中税目数据，按0处理: " + taxItem);
        }

        row.setTaxBaseQuarter(taxBase);
        row.setLevyRatio(levyRatio);
        row.setTaxRate(taxRate);
        row.setActualTaxPayable(actual);
        applyStampQuarterMonthValues(row, taxItem, ctx.getSummaryQuarterSnapshot());
        row.setVarianceReason("");
        return row;
    }

    private void applyStampQuarterMonthValues(SummarySheetDTO.StampDutyItem row,
                                              String taxItem,
                                              SummaryQuarterSnapshot snapshot) {
        if (!isSaleContractItem(taxItem) || snapshot == null || snapshot.getQuarterMonths() == null
                || snapshot.getQuarterMonths().size() < 3 || snapshot.getPlMainRevenueByMonth() == null) {
            row.setTaxBaseMonth1(null);
            row.setTaxBaseMonth2(null);
            row.setTaxBaseMonth3(null);
            return;
        }
        List<String> months = snapshot.getQuarterMonths();
        Map<String, BigDecimal> monthValue = snapshot.getPlMainRevenueByMonth();
        row.setTaxBaseMonth1(monthValue.get(months.get(0)));
        row.setTaxBaseMonth2(monthValue.get(months.get(1)));
        row.setTaxBaseMonth3(monthValue.get(months.get(2)));
    }

    private SummarySheetDTO.CommonTaxItem buildCommonRow(TaxCategoryConfig config,
                                                         String taxType,
                                                         String taxItem,
                                                         Integer rowSeqNo,
                                                         VatBaseLookup vatBaseLookup,
                                                         Map<String, BigDecimal> bookAmountByAccount,
                                                         BigDecimal actualOverride,
                                                         List<String> issues) {
        SummarySheetDTO.CommonTaxItem row = new SummarySheetDTO.CommonTaxItem();
        row.setSeqNo(rowSeqNo);
        row.setTaxType(taxType);
        row.setTaxItem(taxItem);
        row.setTaxBasisDesc(safeText(config.getTaxBasis()));
        row.setAccountCode(safeText(config.getAccountSubject()));

        BigDecimal taxBase = resolveTaxBaseAmount(config, taxType, taxItem, vatBaseLookup, issues);
        BigDecimal levyRatio = normalizeRatio(config.getCollectionRatio());
        BigDecimal taxRate = zeroIfNull(config.getTaxRate());
        BigDecimal actual = actualOverride == null
                ? taxBase.multiply(levyRatio).multiply(taxRate)
                : zeroIfNull(actualOverride);
        BigDecimal book = zeroIfNull(bookAmountByAccount.get(normalizeText(config.getAccountSubject())));
        BigDecimal variance = actual.subtract(book);

        row.setTaxBaseAmount(taxBase);
        row.setLevyRatio(levyRatio);
        row.setTaxRate(taxRate);
        row.setActualTaxPayable(actual);
        row.setBookAmount(book);
        row.setVarianceAmount(variance);
        row.setVarianceReason(variance.signum() == 0 ? "" : DEFAULT_VARIANCE_REASON);
        return row;
    }

    private BigDecimal resolveTaxBaseAmount(TaxCategoryConfig config,
                                            String taxType,
                                            String taxItem,
                                            VatBaseLookup vatBaseLookup,
                                            List<String> issues) {
        if (!isVatTaxType(taxType)) {
            return BigDecimal.ZERO;
        }
        BigDecimal mainRevenueAmount = resolveVatMainRevenueTaxBaseAmount(config, taxItem, vatBaseLookup, issues);
        if (mainRevenueAmount != null) {
            return mainRevenueAmount;
        }
        return zeroIfNull(vatBaseLookup.amountByTaxItem.get(normalizeText(taxItem)));
    }

    private BigDecimal resolveVatMainRevenueTaxBaseAmount(TaxCategoryConfig config,
                                                          String taxItem,
                                                          VatBaseLookup vatBaseLookup,
                                                          List<String> issues) {
        if (!isVatMainRevenueItem(taxItem)) {
            return null;
        }
        String suffix = extractMainRevenueSuffix(taxItem);
        String rateToken = extractRateToken(config == null ? null : config.getTaxRate(), taxItem);

        BigDecimal total = BigDecimal.ZERO;
        int matched = 0;
        for (VatChangeRowDTO row : vatBaseLookup.rows) {
            if (row == null) {
                continue;
            }
            String baseItem = normalizeText(row.getBaseItem());
            String itemName = normalizeText(row.getItemName());
            if (!(baseItem.startsWith(VAT_CHANGE_MAIN_REVENUE_PREFIX) || itemName.startsWith(VAT_CHANGE_MAIN_REVENUE_PREFIX))) {
                continue;
            }
            String joined = baseItem + "|" + itemName + "|" + normalizeText(row.getSplitBasis());
            if (joined.contains("普票")) {
                continue;
            }
            if (!suffix.isBlank() && !joined.contains(suffix)) {
                continue;
            }
            if (!rateToken.isBlank() && !joined.contains(rateToken)) {
                continue;
            }
            matched++;
            total = total.add(zeroIfNull(row.getTotalAmount()));
        }
        if (matched == 0) {
            issues.add("增值税主营业务收入未命中VAT_CHANGE，按0处理: taxItem="
                    + normalizeText(taxItem) + ", suffix=" + suffix + ", rate=" + rateToken);
            return BigDecimal.ZERO;
        }
        return total;
    }

    private boolean isVatMainRevenueItem(String taxItem) {
        String text = normalizeText(taxItem)
                .replace(" ", "")
                .replace("（", "(")
                .replace("）", ")");
        return text.startsWith(VAT_MAIN_REVENUE_PREFIX);
    }

    private String extractMainRevenueSuffix(String taxItem) {
        String text = normalizeText(taxItem)
                .replace(" ", "")
                .replace("（", "(")
                .replace("）", ")");
        if (!text.startsWith(VAT_MAIN_REVENUE_PREFIX)) {
            return "";
        }
        String tail = text.substring(VAT_MAIN_REVENUE_PREFIX.length()).trim();
        if (tail.startsWith("-")) {
            tail = tail.substring(1).trim();
        }
        if (tail.startsWith("(") && tail.endsWith(")") && tail.length() >= 3) {
            tail = tail.substring(1, tail.length() - 1).trim();
        }
        if ("*".equals(tail)) {
            return "";
        }
        return tail;
    }

    private String extractRateToken(BigDecimal taxRate, String taxItem) {
        if (taxRate != null) {
            BigDecimal percent = taxRate;
            if (percent.abs().compareTo(BigDecimal.ONE) <= 0) {
                percent = percent.multiply(BigDecimal.valueOf(100));
            }
            return percent.stripTrailingZeros().toPlainString() + "%";
        }
        String text = normalizeText(taxItem)
                .replace(" ", "")
                .replace("（", "(")
                .replace("）", ")");
        java.util.regex.Matcher matcher = RATE_TOKEN_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + "%";
        }
        return "";
    }

    private SummarySheetDTO.CorporateIncomeTaxItem buildCitRow(TaxCategoryConfig config,
                                                               String taxType,
                                                               String taxItem,
                                                               Map<String, ProjectConfig> projectConfigByTaxCategory) {
        SummarySheetDTO.CorporateIncomeTaxItem row = new SummarySheetDTO.CorporateIncomeTaxItem();
        row.setSeqNo(safeText(config == null ? null : config.getSeqNo()));
        ProjectConfig projectConfig = projectConfigByTaxCategory.get(normalizeText(taxItem));
        String fallbackName = taxItem.isBlank() ? taxType : taxItem;
        row.setProjectName(projectConfig == null ? fallbackName : safeText(projectConfig.getProjectName()));
        row.setPreferentialPeriod(projectConfig == null ? "" : safeText(projectConfig.getPreferentialPeriod()));
        row.setTaxableIncome(BigDecimal.ZERO);
        row.setTaxRate(zeroIfNull(config.getTaxRate()));
        row.setAnnualTaxPayable(BigDecimal.ZERO);
        row.setQ1Tax(BigDecimal.ZERO);
        row.setQ2Tax(BigDecimal.ZERO);
        row.setQ3Tax(BigDecimal.ZERO);
        row.setQ4Tax(BigDecimal.ZERO);
        row.setQ1PayLastYearQ4(BigDecimal.ZERO);
        row.setLossCarryforwardUsed(BigDecimal.ZERO);
        row.setRemainingLossCarryforward(BigDecimal.ZERO);
        return row;
    }

    private List<TaxCategoryConfig> resolveTaxConfigs(LedgerBuildContext ctx) {
        if (ctx.getConfigSnapshot() == null || ctx.getConfigSnapshot().getTaxCategoryConfigs() == null) {
            return List.of();
        }
        String companyCode = normalizeText(ctx.getCompanyCode());
        Map<String, TaxCategoryConfig> dedup = new LinkedHashMap<>();
        for (TaxCategoryConfig config : ctx.getConfigSnapshot().getTaxCategoryConfigs()) {
            if (config == null) {
                continue;
            }
            String cfgCompany = normalizeText(config.getCompanyCode());
            if (!(cfgCompany.isEmpty() || cfgCompany.equals(companyCode))) {
                continue;
            }
            String key = normalizeText(config.getTaxType()) + "|" + normalizeText(config.getTaxCategory());
            TaxCategoryConfig current = dedup.get(key);
            if (current == null) {
                dedup.put(key, config);
                continue;
            }
            String currentCompany = normalizeText(current.getCompanyCode());
            if (currentCompany.isEmpty() && !cfgCompany.isEmpty()) {
                dedup.put(key, config);
            }
        }
        return dedup.values().stream()
                .sorted(Comparator.comparing(this::toSeqOrderKey)
                        .thenComparing(cfg -> normalizeText(cfg == null ? null : cfg.getSeqNo()))
                        .thenComparing(cfg -> normalizeText(cfg == null ? null : cfg.getTaxCategory())))
                .collect(Collectors.toList());
    }

    private Map<String, StampAgg> resolveStampAggMap(LedgerBuildContext ctx, List<String> issues) {
        Map<String, StampAgg> result = new LinkedHashMap<>();
        if ("2320".equals(ctx.getCompanyCode()) || "2355".equals(ctx.getCompanyCode())) {
            if (!ctx.hasParsed(FileCategoryEnum.STAMP_TAX)) {
                issues.add("缺少印花税解析数据(STAMP_TAX)，按0处理");
                return result;
            }
            List<StampDutySummaryRowDTO> rows = ctx.getParsedList(FileCategoryEnum.STAMP_TAX, StampDutySummaryRowDTO.class);
            for (StampDutySummaryRowDTO row : rows) {
                if (row == null) {
                    continue;
                }
                String key = normalizeText(row.getContractCategory());
                if (key.isEmpty()) {
                    continue;
                }
                StampAgg agg = result.computeIfAbsent(key, k -> new StampAgg());
                agg.taxableAmount = agg.taxableAmount.add(zeroIfNull(row.getTaxableAmount()));
                agg.taxPayableAmount = agg.taxPayableAmount.add(zeroIfNull(row.getTaxPayableAmount()));
                if (agg.taxRate.signum() == 0 && row.getTaxRate() != null) {
                    agg.taxRate = row.getTaxRate();
                }
            }
            return result;
        }
        if (!ctx.hasParsed(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER)) {
            issues.add("缺少合同印花税明细解析数据(CONTRACT_STAMP_DUTY_LEDGER)，按0处理");
            return result;
        }
        List<ContractStampDutyLedgerItemDTO> rows = ctx.getParsedList(FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER, ContractStampDutyLedgerItemDTO.class);
        String quarter = quarterOf(normalizeYearMonth(ctx.getYearMonth()));
        for (ContractStampDutyLedgerItemDTO row : rows) {
            if (row == null) {
                continue;
            }
            String rowQuarter = normalizeText(row.getQuarter()).toUpperCase(Locale.ROOT);
            if (!rowQuarter.equals(quarter)) {
                continue;
            }
            String key = normalizeText(row.getStampDutyTaxItem());
            if (key.isEmpty()) {
                continue;
            }
            StampAgg agg = result.computeIfAbsent(key, k -> new StampAgg());
            agg.taxableAmount = agg.taxableAmount.add(zeroIfNull(row.getContractAmount()));
            agg.taxPayableAmount = agg.taxPayableAmount.add(zeroIfNull(row.getTaxableAmount()));
            if (agg.taxRate.signum() == 0 && row.getStampDutyTaxRate() != null) {
                agg.taxRate = row.getStampDutyTaxRate();
            }
        }
        return result;
    }

    private VatBaseLookup resolveVatBaseLookup(LedgerBuildContext ctx) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        List<VatChangeRowDTO> payload = List.of();
        VatChangeLedgerSheetData vatChangeData =
                ctx.requireBuilt(LedgerSheetCode.VAT_CHANGE, VatChangeLedgerSheetData.class, support());
        if (vatChangeData.getPayload() == null) {
            return new VatBaseLookup(result, payload);
        }
        List<VatChangeRowDTO> rows = vatChangeData.getPayload();
        payload = rows;
        for (VatChangeRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            BigDecimal amount = zeroIfNull(row.getTotalAmount());
            mergeAmount(result, normalizeText(row.getBaseItem()), amount);
            mergeAmount(result, normalizeText(row.getItemName()), amount);
        }
        return new VatBaseLookup(result, payload);
    }

    private Map<String, BigDecimal> resolveBookAmountByAccount(LedgerBuildContext ctx) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (ctx.hasParsed(FileCategoryEnum.DL_INPUT)) {
            DlInputParsedDTO dlInput = ctx.getParsedObject(FileCategoryEnum.DL_INPUT, DlInputParsedDTO.class);
            if (dlInput != null && dlInput.getLocalAmountSumByAccount() != null) {
                dlInput.getLocalAmountSumByAccount().forEach((k, v) -> mergeAmount(result, normalizeText(k), zeroIfNull(v)));
            }
        }
        if (ctx.hasParsed(FileCategoryEnum.DL_OTHER)) {
            DlOtherParsedDTO dlOther = ctx.getParsedObject(FileCategoryEnum.DL_OTHER, DlOtherParsedDTO.class);
            if (dlOther != null) {
                if (dlOther.getLocalAmountSumByAccount() != null) {
                    dlOther.getLocalAmountSumByAccount().forEach((k, v) -> mergeAmount(result, normalizeText(k), zeroIfNull(v)));
                }
                if (dlOther.getDocumentAmountSumByAccount() != null) {
                    dlOther.getDocumentAmountSumByAccount().forEach((k, v) -> mergeAmount(result, normalizeText(k), zeroIfNull(v)));
                }
            }
        }
        return result;
    }

    private Map<String, ProjectConfig> resolveProjectConfigByTaxCategory(LedgerBuildContext ctx) {
        if (ctx.getConfigSnapshot() == null || ctx.getConfigSnapshot().getProjectConfigs() == null) {
            return Map.of();
        }
        Map<String, ProjectConfig> result = new LinkedHashMap<>();
        for (ProjectConfig projectConfig : ctx.getConfigSnapshot().getProjectConfigs()) {
            if (projectConfig == null) {
                continue;
            }
            String taxCategory = normalizeText(projectConfig.getTaxCategory());
            if (taxCategory.isEmpty()) {
                continue;
            }
            result.putIfAbsent(taxCategory, projectConfig);
        }
        return result;
    }

    private String resolveCompanyName(LedgerBuildContext ctx) {
        if (ctx.getConfigSnapshot() != null && ctx.getConfigSnapshot().getCurrentCompany() != null) {
            String companyName = ctx.getConfigSnapshot().getCurrentCompany().getCompanyName();
            if (companyName != null && !companyName.isBlank()) {
                return companyName;
            }
        }
        return ctx.getCompanyCode();
    }

    private void appendBuildIssues(LedgerBuildContext ctx, List<String> issues) {
        if (issues.isEmpty() || ctx.getPreloadSummary() == null) {
            return;
        }
        LinkedHashSet<String> all = new LinkedHashSet<>();
        Object existing = ctx.getPreloadSummary().get("summaryBuildIssues");
        if (existing instanceof List<?>) {
            List<?> list = (List<?>) existing;
            for (Object item : list) {
                if (item != null) {
                    all.add(item.toString());
                }
            }
        }
        all.addAll(issues);
        ctx.getPreloadSummary().put("summaryBuildIssues", new ArrayList<>(all));
        log.warn("summary build issues: companyCode={}, yearMonth={}, issues={}",
                ctx.getCompanyCode(), ctx.getYearMonth(), issues);
    }

    private String normalizeYearMonth(String yearMonth) {
        if (yearMonth == null) {
            return "";
        }
        String text = yearMonth.trim();
        if (text.matches("^\\d{6}$")) {
            text = text.substring(0, 4) + "-" + text.substring(4, 6);
        }
        if (!text.matches("^\\d{4}-\\d{2}$")) {
            return text;
        }
        return YearMonth.parse(text).toString();
    }

    private String quarterOf(String normalizedYearMonth) {
        int month = YearMonth.parse(normalizedYearMonth).getMonthValue();
        return "Q" + ((month - 1) / 3 + 1);
    }

    private SeqOrderKey toSeqOrderKey(TaxCategoryConfig cfg) {
        String rawSeq = normalizeText(cfg == null ? null : cfg.getSeqNo());
        if (rawSeq.isBlank() || !SEQ_NO_PATTERN.matcher(rawSeq).matches()) {
            return SeqOrderKey.invalid(rawSeq);
        }
        try {
            String[] parts = rawSeq.split("\\.", 2);
            BigInteger intPart = new BigInteger(parts[0]);
            if (parts.length == 1) {
                return SeqOrderKey.integer(rawSeq, intPart);
            }
            String fracRaw = parts[1];
            if (fracRaw == null || fracRaw.isBlank()) {
                return SeqOrderKey.integer(rawSeq, intPart);
            }
            if (fracRaw.chars().allMatch(ch -> ch == '0')) {
                return SeqOrderKey.integer(rawSeq, intPart);
            }
            return SeqOrderKey.decimal(rawSeq, intPart, new BigInteger(fracRaw));
        } catch (Exception ignore) {
            return SeqOrderKey.invalid(rawSeq);
        }
    }

    private boolean isIntegerSeq(String seqNo) {
        if (seqNo == null || seqNo.isBlank()) {
            return false;
        }
        try {
            BigDecimal value = new BigDecimal(seqNo.trim()).stripTrailingZeros();
            return value.scale() <= 0;
        } catch (Exception ignore) {
            return false;
        }
    }

    private BigDecimal normalizeRatio(BigDecimal ratio) {
        if (ratio == null || ratio.signum() == 0) {
            return ONE;
        }
        return ratio;
    }

    private void mergeAmount(Map<String, BigDecimal> map, String key, BigDecimal value) {
        if (key == null || key.isBlank()) {
            return;
        }
        map.merge(key, zeroIfNull(value), BigDecimal::add);
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim();
    }

    private String safeText(String value) {
        return Objects.requireNonNullElse(normalizeText(value), "");
    }

    private boolean isSaleContractItem(String taxItem) {
        if (taxItem == null) {
            return false;
        }
        String text = taxItem.replace(" ", "").trim();
        return "买卖合同".equals(text);
    }

    private boolean isVatTaxType(String taxType) {
        return normalizeText(taxType).contains("增值税");
    }

    private boolean isSubtotalLine(String taxType, String taxItem) {
        return normalizeText(taxType).contains("合计") || normalizeText(taxItem).contains("合计");
    }

    private BigDecimal resolveVatSubtotalDeclaredAmount(LedgerBuildContext ctx, List<String> issues) {
        VatChangeLedgerSheetData vatChangeData;
        try {
            vatChangeData = ctx.requireBuilt(LedgerSheetCode.VAT_CHANGE, VatChangeLedgerSheetData.class, support());
        } catch (Exception ex) {
            issues.add(VAT_SUBTOTAL_FALLBACK_ISSUE);
            return BigDecimal.ZERO;
        }
        List<VatChangeRowDTO> rows = vatChangeData.getPayload();
        if (rows == null || rows.isEmpty()) {
            issues.add(VAT_SUBTOTAL_FALLBACK_ISSUE);
            return BigDecimal.ZERO;
        }
        for (VatChangeRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (VAT_PAYABLE_ITEM.equals(normalizeText(row.getItemName()))) {
                return zeroIfNull(row.getTotalAmount());
            }
        }
        for (VatChangeRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (VAT_PAYABLE_ITEM.equals(normalizeText(row.getBaseItem()))) {
                return zeroIfNull(row.getTotalAmount());
            }
        }
        issues.add(VAT_SUBTOTAL_FALLBACK_ISSUE);
        return BigDecimal.ZERO;
    }

    private static class StampAgg {
        private BigDecimal taxableAmount = BigDecimal.ZERO;
        private BigDecimal taxRate = BigDecimal.ZERO;
        private BigDecimal taxPayableAmount = BigDecimal.ZERO;
    }

    private static class VatBaseLookup {
        private final Map<String, BigDecimal> amountByTaxItem;
        private final List<VatChangeRowDTO> rows;

        private VatBaseLookup(Map<String, BigDecimal> amountByTaxItem, List<VatChangeRowDTO> rows) {
            this.amountByTaxItem = amountByTaxItem;
            this.rows = rows;
        }
    }

    private static class SeqOrderKey implements Comparable<SeqOrderKey> {
        private final int validRank;
        private final BigInteger intPart;
        private final int typeRank;
        private final BigInteger fracPart;
        private final String rawSeq;

        private SeqOrderKey(int validRank,
                            BigInteger intPart,
                            int typeRank,
                            BigInteger fracPart,
                            String rawSeq) {
            this.validRank = validRank;
            this.intPart = intPart;
            this.typeRank = typeRank;
            this.fracPart = fracPart;
            this.rawSeq = rawSeq == null ? "" : rawSeq;
        }

        private static SeqOrderKey invalid(String rawSeq) {
            return new SeqOrderKey(1, BigInteger.valueOf(Long.MAX_VALUE), 1, BigInteger.ZERO, rawSeq);
        }

        private static SeqOrderKey decimal(String rawSeq, BigInteger intPart, BigInteger fracPart) {
            return new SeqOrderKey(0, intPart, 0, fracPart, rawSeq);
        }

        private static SeqOrderKey integer(String rawSeq, BigInteger intPart) {
            return new SeqOrderKey(0, intPart, 1, BigInteger.ZERO, rawSeq);
        }

        @Override
        public int compareTo(SeqOrderKey other) {
            int cmp = Integer.compare(this.validRank, other.validRank);
            if (cmp != 0) {
                return cmp;
            }
            cmp = this.intPart.compareTo(other.intPart);
            if (cmp != 0) {
                return cmp;
            }
            cmp = Integer.compare(this.typeRank, other.typeRank);
            if (cmp != 0) {
                return cmp;
            }
            cmp = this.fracPart.compareTo(other.fracPart);
            if (cmp != 0) {
                return cmp;
            }
            return this.rawSeq.compareTo(other.rawSeq);
        }
    }
}
