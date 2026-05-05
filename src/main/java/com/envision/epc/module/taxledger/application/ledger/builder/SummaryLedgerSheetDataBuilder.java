package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import com.envision.epc.module.taxledger.application.dto.BsAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.DlInputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOtherParsedDTO;
import com.envision.epc.module.taxledger.application.dto.DlOutputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.SummarySheetDTO;
import com.envision.epc.module.taxledger.application.dto.StampDutySummaryRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatInputCertParsedDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.ledger.CitQuarterAmountDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.SummaryQuarterSnapshot;
import com.envision.epc.module.taxledger.application.ledger.data.PlAppendix2320LedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.data.SummaryLedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import com.envision.epc.module.taxledger.application.service.DataLakeService;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.ProjectConfig;
import com.envision.epc.module.taxledger.domain.TaxCategoryConfig;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class SummaryLedgerSheetDataBuilder implements LedgerSheetDataBuilder<SummaryLedgerSheetData> {
    private static final BigDecimal ONE = BigDecimal.ONE;
    private static final Pattern SEQ_NO_PATTERN = Pattern.compile("^\\d+(\\.\\d+)?$");
    private static final Pattern RATE_TOKEN_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)%");
    private static final String VAT_PAYABLE_ITEM = "应交增值税";
    private static final String VAT_SUBTOTAL_FALLBACK_ISSUE = "增值税合计未命中VAT_CHANGE条目=应交增值税，按0处理";
    private static final String VAT_MAIN_REVENUE_PREFIX = "销项税额-主营业务收入";
    private static final String VAT_CHANGE_MAIN_REVENUE_PREFIX = "当月利润表主营业务收入-";
    private static final String VAT_INPUT_TAX_ITEM = "增值税进项税额";
    private static final String VAT_ENDING_INPUT_CARRY = "期末进项留抵金额";
    private static final String VAT_BEGINNING_INPUT_CARRY = "期初进项留抵金额";
    private static final String VAT_CHANGE_CERTIFIED_INPUT_TAX = "增值税已认证进项税";
    private static final String VAT_CHANGE_INPUT_TRANSFER_OUT = "进项转出";
    private static final String VAT_CHANGE_BEGINNING_INPUT_CARRY_PREFIX = "期初留抵进项税";
    private static final String VAT_CHANGE_ENDING_INPUT_CARRY_PREFIX = "期末留抵进项税";
    private static final String BS_APPENDIX_TAX_PAYABLE_VAT_PREFIX = "应交税费-增值税";
    private static final String CIT_TAX_TYPE = "企业所得税";
    private static final String CIT_ITEM_PROFIT_TOTAL = "利润总额";
    private static final String CIT_PERIOD_REDUCTION = "减免期";
    private static final String CIT_PERIOD_HALF = "减半期";

    private final DataLakeService dataLakeService;

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
        VatBookLookup vatBookLookup = resolveVatBookLookup(ctx, issues);
        Map<String, BigDecimal> bookAmountByAccount = resolveBookAmountByAccount(ctx);

        List<SummarySheetDTO.StampDutyItem> stampRows = new ArrayList<>();
        List<SummarySheetDTO.CommonTaxItem> vatRows = new ArrayList<>();
        List<SummarySheetDTO.CommonTaxItem> commonRows = new ArrayList<>();
        List<SummarySheetDTO.CorporateIncomeTaxItem> citRows;
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
            if (normalizedTaxType.contains(CIT_TAX_TYPE)) {
                continue;
            }
            BigDecimal actualOverride = null;
            if (isVatTaxType(taxType)) {
                actualOverride = resolveVatInputTaxDeclaredAmount(taxItem, vatBaseLookup);
                if (actualOverride == null && isSubtotalLine(taxType, taxItem)) {
                    if (!vatSubtotalResolved) {
                        vatSubtotalDeclared = resolveVatSubtotalDeclaredAmount(ctx, issues);
                        vatSubtotalResolved = true;
                    }
                    actualOverride = vatSubtotalDeclared;
                }
            }
            SummarySheetDTO.CommonTaxItem row = buildCommonRow(
                    config, taxType, taxItem, rowSeqNo, vatBaseLookup, vatBookLookup, bookAmountByAccount, actualOverride, issues);
            if (isVatTaxType(taxType)) {
                vatRows.add(row);
            } else {
                commonRows.add(row);
            }
        }
        citRows = buildCitRows(ctx, taxConfigs, issues);

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
                                                         VatBookLookup vatBookLookup,
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
        BigDecimal taxBaseForCalc = zeroIfNull(taxBase);
        BigDecimal levyRatio = normalizeRatio(config.getCollectionRatio());
        BigDecimal taxRate = zeroIfNull(config.getTaxRate());
        BigDecimal actual = actualOverride == null
                ? taxBaseForCalc.multiply(levyRatio).multiply(taxRate)
                : zeroIfNull(actualOverride);
        BigDecimal book = resolveBookAmount(config, taxType, taxItem, vatBookLookup, bookAmountByAccount, issues);
        BigDecimal variance = actual.subtract(zeroIfNull(book));

        row.setTaxBaseAmount(taxBase);
        row.setLevyRatio(levyRatio);
        row.setTaxRate(taxRate);
        row.setActualTaxPayable(actual);
        row.setBookAmount(book);
        row.setVarianceAmount(variance);
        row.setVarianceReason("");
        return row;
    }

    private BigDecimal resolveBookAmount(TaxCategoryConfig config,
                                         String taxType,
                                         String taxItem,
                                         VatBookLookup vatBookLookup,
                                         Map<String, BigDecimal> bookAmountByAccount,
                                         List<String> issues) {
        if (!isVatTaxType(taxType)) {
            return zeroIfNull(bookAmountByAccount.get(normalizeText(config == null ? null : config.getAccountSubject())));
        }
        return resolveVatBookAmount(taxItem, vatBookLookup, issues);
    }

    private BigDecimal resolveVatBookAmount(String taxItem, VatBookLookup vatBookLookup, List<String> issues) {
        if (vatBookLookup == null) {
            return null;
        }
        String normalizedTaxItem = normalizeText(taxItem);
        boolean rjjc = vatBookLookup.rjjcCompany;

        if (VAT_INPUT_TAX_ITEM.equals(normalizedTaxItem)) {
            return zeroIfNull(vatBookLookup.dlInputDocumentAmountSum);
        }
        if (VAT_ENDING_INPUT_CARRY.equals(normalizedTaxItem)) {
            return zeroIfNull(vatBookLookup.bsAppendixVatCumulativeBalance);
        }
        if (normalizedTaxItem.startsWith(VAT_MAIN_REVENUE_PREFIX)) {
            if (rjjc) {
                return resolveRjjcMainRevenueBookBySuffix(normalizedTaxItem, vatBookLookup, issues);
            }
            if (isNonRjjcMainRevenueItem(normalizedTaxItem)) {
                return zeroIfNull(vatBookLookup.dlOutputDocumentAmountSum).negate();
            }
            return null;
        }
        return null;
    }

    private boolean isNonRjjcMainRevenueItem(String normalizedTaxItem) {
        return VAT_MAIN_REVENUE_PREFIX.equals(normalizedTaxItem)
                || (VAT_MAIN_REVENUE_PREFIX + "*").equals(normalizedTaxItem);
    }

    private BigDecimal resolveRjjcMainRevenueBookBySuffix(String taxItem,
                                                          VatBookLookup vatBookLookup,
                                                          List<String> issues) {
        String suffix = extractMainRevenueSuffix(taxItem);
        if (suffix.isBlank()) {
            issues.add("增值税账面金额税目后缀提取失败，按空处理: taxItem=" + taxItem);
            return null;
        }
        BigDecimal total = BigDecimal.ZERO;
        int matched = 0;
        for (Map.Entry<String, BigDecimal> entry : vatBookLookup.plAppendixDeclaredTaxBySplitBasis.entrySet()) {
            String splitBasis = normalizeText(entry.getKey());
            if (splitBasis.contains("普票")) {
                continue;
            }
            if (!splitBasis.contains(suffix)) {
                continue;
            }
            matched++;
            total = total.add(zeroIfNull(entry.getValue()));
        }
        if (matched == 0) {
            issues.add("增值税账面金额未命中PL附表拆分依据，按空处理: taxItem=" + taxItem + ", suffix=" + suffix);
            return null;
        }
        return total;
    }

    private BigDecimal resolveTaxBaseAmount(TaxCategoryConfig config,
                                            String taxType,
                                            String taxItem,
                                            VatBaseLookup vatBaseLookup,
                                            List<String> issues) {
        if (!isVatTaxType(taxType)) {
            return BigDecimal.ZERO;
        }
        if (isVatCarryAmountEmptyItem(taxItem)) {
            return null;
        }
        BigDecimal vatInputCertAmount = resolveVatInputCertAmount(taxItem, vatBaseLookup);
        if (vatInputCertAmount != null) {
            return vatInputCertAmount;
        }
        BigDecimal mainRevenueAmount = resolveVatMainRevenueTaxBaseAmount(config, taxItem, vatBaseLookup, issues);
        if (mainRevenueAmount != null) {
            return mainRevenueAmount;
        }
        return zeroIfNull(vatBaseLookup.amountByTaxItem.get(normalizeText(taxItem)));
    }

    private boolean isVatCarryAmountEmptyItem(String taxItem) {
        String item = normalizeText(taxItem);
        return VAT_ENDING_INPUT_CARRY.equals(item) || VAT_BEGINNING_INPUT_CARRY.equals(item);
    }

    private BigDecimal resolveVatInputCertAmount(String taxItem, VatBaseLookup vatBaseLookup) {
        if (!VAT_INPUT_TAX_ITEM.equals(normalizeText(taxItem))) {
            return null;
        }
        return zeroIfNull(vatBaseLookup.vatInputCertAmount);
    }

    private BigDecimal resolveVatInputTaxDeclaredAmount(String taxItem, VatBaseLookup vatBaseLookup) {
        String normalizedTaxItem = normalizeText(taxItem);
        if (VAT_INPUT_TAX_ITEM.equals(normalizedTaxItem)) {
            BigDecimal declared = BigDecimal.ZERO;
            for (VatChangeRowDTO row : vatBaseLookup.rows) {
                if (row == null) {
                    continue;
                }
                String baseItem = normalizeText(row.getBaseItem());
                String itemName = normalizeText(row.getItemName());
                if (VAT_CHANGE_CERTIFIED_INPUT_TAX.equals(baseItem)
                        || VAT_CHANGE_CERTIFIED_INPUT_TAX.equals(itemName)
                        || VAT_CHANGE_INPUT_TRANSFER_OUT.equals(baseItem)
                        || VAT_CHANGE_INPUT_TRANSFER_OUT.equals(itemName)) {
                    declared = declared.add(zeroIfNull(row.getTotalAmount()));
                }
            }
            return declared;
        }
        if (VAT_BEGINNING_INPUT_CARRY.equals(normalizedTaxItem)) {
            return sumVatChangeByPrefix(vatBaseLookup.rows, VAT_CHANGE_BEGINNING_INPUT_CARRY_PREFIX);
        }
        if (VAT_ENDING_INPUT_CARRY.equals(normalizedTaxItem)) {
            return sumVatChangeByPrefix(vatBaseLookup.rows, VAT_CHANGE_ENDING_INPUT_CARRY_PREFIX);
        }
        return null;
    }

    private BigDecimal sumVatChangeByPrefix(List<VatChangeRowDTO> rows, String prefix) {
        BigDecimal total = BigDecimal.ZERO;
        for (VatChangeRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            String baseItem = normalizeText(row.getBaseItem());
            String itemName = normalizeText(row.getItemName());
            if (baseItem.startsWith(prefix) || itemName.startsWith(prefix)) {
                total = total.add(zeroIfNull(row.getTotalAmount()));
            }
        }
        return total;
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

    private List<SummarySheetDTO.CorporateIncomeTaxItem> buildCitRows(LedgerBuildContext ctx,
                                                                       List<TaxCategoryConfig> taxConfigs,
                                                                       List<String> issues) {
        List<ProjectConfig> projects = resolveCitProjects(ctx);
        if (projects.isEmpty()) {
            return List.of();
        }

        BigDecimal taxRate = resolveCitTaxRate(taxConfigs, issues);
        BigDecimal firstTaxableIncome = resolveCitTaxableIncome(ctx, issues);
        CitQuarterAmountDTO quarterAmount = loadCitQuarterAmounts(ctx, issues);

        List<SummarySheetDTO.CorporateIncomeTaxItem> rows = new ArrayList<>();
        for (int i = 0; i < projects.size(); i++) {
            ProjectConfig project = projects.get(i);
            SummarySheetDTO.CorporateIncomeTaxItem row = new SummarySheetDTO.CorporateIncomeTaxItem();
            row.setSeqNo("");
            row.setProjectName(safeText(project.getProjectName()));
            row.setPreferentialPeriod(safeText(project.getPreferentialPeriod()));

            BigDecimal taxableIncome = i == 0 ? firstTaxableIncome : null;
            row.setTaxableIncome(taxableIncome);
            row.setTaxRate(taxRate);
            row.setAnnualTaxPayable(calculateCitAnnualPayable(taxableIncome, taxRate, row.getPreferentialPeriod()));

            if (i == 0) {
                row.setQ1Tax(zeroIfNull(quarterAmount.getQ1()));
                row.setQ2Tax(zeroIfNull(quarterAmount.getQ2()));
                row.setQ3Tax(zeroIfNull(quarterAmount.getQ3()));
                row.setQ4Tax(zeroIfNull(quarterAmount.getQ4()));
            } else {
                row.setQ1Tax(null);
                row.setQ2Tax(null);
                row.setQ3Tax(null);
                row.setQ4Tax(null);
            }

            // 由用户手填
            row.setQ1PayLastYearQ4(null);
            row.setLossCarryforwardUsed(null);
            // render 层写联动公式
            row.setRemainingLossCarryforward(null);
            rows.add(row);
        }
        return rows;
    }

    private List<ProjectConfig> resolveCitProjects(LedgerBuildContext ctx) {
        if (ctx.getConfigSnapshot() == null || ctx.getConfigSnapshot().getProjectConfigs() == null) {
            return List.of();
        }
        String companyCode = normalizeText(ctx.getCompanyCode());
        Map<String, ProjectConfig> dedup = new LinkedHashMap<>();
        for (ProjectConfig projectConfig : ctx.getConfigSnapshot().getProjectConfigs()) {
            if (projectConfig == null) {
                continue;
            }
            String taxType = normalizeText(projectConfig.getTaxType());
            if (!taxType.contains(CIT_TAX_TYPE)) {
                continue;
            }
            String cfgCompany = normalizeText(projectConfig.getCompanyCode());
            if (!(cfgCompany.isEmpty() || cfgCompany.equals(companyCode))) {
                continue;
            }
            String projectName = normalizeText(projectConfig.getProjectName());
            if (projectName.isEmpty()) {
                continue;
            }
            ProjectConfig current = dedup.get(projectName);
            if (current == null) {
                dedup.put(projectName, projectConfig);
                continue;
            }
            String currentCompany = normalizeText(current.getCompanyCode());
            if (currentCompany.isEmpty() && !cfgCompany.isEmpty()) {
                dedup.put(projectName, projectConfig);
            }
        }
        return new ArrayList<>(dedup.values());
    }

    private BigDecimal resolveCitTaxRate(List<TaxCategoryConfig> taxConfigs, List<String> issues) {
        if (taxConfigs == null || taxConfigs.isEmpty()) {
            issues.add("企业所得税税率缺失，按0处理");
            return BigDecimal.ZERO;
        }
        for (TaxCategoryConfig config : taxConfigs) {
            if (config == null) {
                continue;
            }
            String taxType = normalizeText(config.getTaxType());
            if (!taxType.contains(CIT_TAX_TYPE)) {
                continue;
            }
            if (config.getTaxRate() != null) {
                return config.getTaxRate();
            }
        }
        issues.add("企业所得税税率缺失，按0处理");
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveCitTaxableIncome(LedgerBuildContext ctx, List<String> issues) {
        if (!ctx.hasParsed(FileCategoryEnum.PL)) {
            issues.add("企业所得税应纳税所得额缺少PL解析结果，按空处理");
            return null;
        }
        List<PlStatementRowDTO> rows = ctx.getParsedList(FileCategoryEnum.PL, PlStatementRowDTO.class);
        for (PlStatementRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            String itemName = normalizeText(row.getItemName());
            if (!itemName.contains(CIT_ITEM_PROFIT_TOTAL)) {
                continue;
            }
            return row.getAccumulatedAmount();
        }
        issues.add("企业所得税应纳税所得额未命中PL条目=利润总额，按空处理");
        return null;
    }

    private CitQuarterAmountDTO loadCitQuarterAmounts(LedgerBuildContext ctx, List<String> issues) {
        try {
            return dataLakeService.loadCitQuarterAmounts(ctx.getYearMonth(), ctx.getCompanyCode());
        } catch (Exception ex) {
            issues.add("企业所得税季度数据加载失败，按0处理");
            return new CitQuarterAmountDTO();
        }
    }

    private BigDecimal calculateCitAnnualPayable(BigDecimal taxableIncome, BigDecimal taxRate, String preferentialPeriod) {
        BigDecimal base = zeroIfNull(taxableIncome);
        BigDecimal rate = zeroIfNull(taxRate);
        String period = normalizeText(preferentialPeriod);
        if (period.contains(CIT_PERIOD_REDUCTION)) {
            return BigDecimal.ZERO;
        }
        if (period.contains(CIT_PERIOD_HALF)) {
            return base.multiply(rate).multiply(BigDecimal.valueOf(0.5)).setScale(2, RoundingMode.HALF_UP);
        }
        return base.multiply(rate).setScale(2, RoundingMode.HALF_UP);
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
        BigDecimal vatInputCertAmount = BigDecimal.ZERO;
        if (ctx.hasParsed(FileCategoryEnum.VAT_INPUT_CERT)) {
            VatInputCertParsedDTO vatInputCert = ctx.getParsedObject(FileCategoryEnum.VAT_INPUT_CERT, VatInputCertParsedDTO.class);
            vatInputCertAmount = zeroIfNull(vatInputCert == null ? null : vatInputCert.getAmountSum());
        }
        VatChangeLedgerSheetData vatChangeData =
                ctx.requireBuilt(LedgerSheetCode.VAT_CHANGE, VatChangeLedgerSheetData.class, support());
        if (vatChangeData.getPayload() == null) {
            return new VatBaseLookup(result, payload, vatInputCertAmount);
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
        return new VatBaseLookup(result, payload, vatInputCertAmount);
    }

    private VatBookLookup resolveVatBookLookup(LedgerBuildContext ctx, List<String> issues) {
        VatBookLookup lookup = new VatBookLookup();
        lookup.rjjcCompany = isCompany2320Or2355(ctx.getCompanyCode());

        if (ctx.hasParsed(FileCategoryEnum.DL_OUTPUT)) {
            DlOutputParsedDTO dlOutput = ctx.getParsedObject(FileCategoryEnum.DL_OUTPUT, DlOutputParsedDTO.class);
            lookup.dlOutputDocumentAmountSum = zeroIfNull(dlOutput == null ? null : dlOutput.getDocumentAmountSum());
        } else {
            issues.add("增值税账面金额缺少DL_OUTPUT解析结果，相关税目按空/0处理");
        }

        if (ctx.hasParsed(FileCategoryEnum.DL_INPUT)) {
            DlInputParsedDTO dlInput = ctx.getParsedObject(FileCategoryEnum.DL_INPUT, DlInputParsedDTO.class);
            lookup.dlInputDocumentAmountSum = zeroIfNull(dlInput == null ? null : dlInput.getDocumentAmountSum());
        } else {
            issues.add("增值税账面金额缺少DL_INPUT解析结果，相关税目按空/0处理");
        }

        if (ctx.hasParsed(FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE)) {
            List<BsAppendixUploadDTO> rows = ctx.getParsedList(FileCategoryEnum.BS_APPENDIX_TAX_PAYABLE, BsAppendixUploadDTO.class);
            lookup.bsAppendixVatCumulativeBalance = sumBsAppendixVatCumulativeBalance(rows);
        } else {
            issues.add("增值税账面金额缺少BS附表解析结果，期末进项留抵金额按空/0处理");
        }

        if (lookup.rjjcCompany) {
            try {
                PlAppendix2320LedgerSheetData plAppendixData = ctx.requireBuilt(
                        LedgerSheetCode.PL_APPENDIX_2320, PlAppendix2320LedgerSheetData.class, support());
                PlAppendix23202355DTO dto = plAppendixData == null ? null : plAppendixData.getPayload();
                lookup.plAppendixDeclaredTaxBySplitBasis = mapDeclaredTaxBySplitBasis(dto);
            } catch (Exception ex) {
                issues.add("增值税账面金额缺少PL附表2320 builder产物，主营业务收入税目按空处理");
            }
        }
        return lookup;
    }

    private BigDecimal sumBsAppendixVatCumulativeBalance(List<BsAppendixUploadDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (BsAppendixUploadDTO row : rows) {
            if (row == null) {
                continue;
            }
            String shortText = normalizeText(row.getShortText());
            String account = normalizeText(row.getGlAccount());
            String carrier = shortText + "|" + account;
            if (!carrier.contains(BS_APPENDIX_TAX_PAYABLE_VAT_PREFIX)) {
                continue;
            }
            total = total.add(parseLooseNumber(row.getCumulativeBalance()));
        }
        return total;
    }

    private Map<String, BigDecimal> mapDeclaredTaxBySplitBasis(PlAppendix23202355DTO dto) {
        Map<String, BigDecimal> result = new LinkedHashMap<>();
        if (dto == null || dto.getDeclarationSplitList() == null) {
            return result;
        }
        for (PlAppendix23202355DTO.DeclarationSplitItem row : dto.getDeclarationSplitList()) {
            if (row == null) {
                continue;
            }
            String splitBasis = normalizeText(row.getSplitBasis());
            if (splitBasis.isBlank()) {
                continue;
            }
            result.merge(splitBasis, zeroIfNull(row.getDeclaredTaxAmount()), BigDecimal::add);
        }
        return result;
    }

    private BigDecimal parseLooseNumber(String raw) {
        String text = normalizeText(raw);
        if (text.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = text.replace(",", "");
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            normalized = "-" + normalized.substring(1, normalized.length() - 1);
        }
        try {
            return new BigDecimal(normalized);
        } catch (Exception ex) {
            return BigDecimal.ZERO;
        }
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

    private boolean isCompany2320Or2355(String companyCode) {
        String code = normalizeText(companyCode);
        return "2320".equals(code) || "2355".equals(code);
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
        private final BigDecimal vatInputCertAmount;

        private VatBaseLookup(Map<String, BigDecimal> amountByTaxItem, List<VatChangeRowDTO> rows, BigDecimal vatInputCertAmount) {
            this.amountByTaxItem = amountByTaxItem;
            this.rows = rows;
            this.vatInputCertAmount = vatInputCertAmount;
        }
    }

    private static class VatBookLookup {
        private boolean rjjcCompany;
        private BigDecimal dlOutputDocumentAmountSum = BigDecimal.ZERO;
        private BigDecimal dlInputDocumentAmountSum = BigDecimal.ZERO;
        private BigDecimal bsAppendixVatCumulativeBalance = BigDecimal.ZERO;
        private Map<String, BigDecimal> plAppendixDeclaredTaxBySplitBasis = new LinkedHashMap<>();
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
