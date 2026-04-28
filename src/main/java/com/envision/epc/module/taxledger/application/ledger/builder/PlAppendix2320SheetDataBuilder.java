package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.MonthlySettlementTaxParsedDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.PlAppendix2320LedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PL附表（2320/2355公司） 页数据构建器。
 * 仅依赖 preloadedParsedData：PL附表 + 月结聚合结果。
 */
@Component
@Slf4j
public class PlAppendix2320SheetDataBuilder implements LedgerSheetDataBuilder<PlAppendix2320LedgerSheetData> {
    private static final Pattern TAX_RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]");

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.PL_APPENDIX_2320;
    }

    @Override
    public PlAppendix2320LedgerSheetData build(LedgerBuildContext ctx) {
        PlAppendix23202355DTO source = SheetDataReaders.requireObjectPreloaded(
                ctx, FileCategoryEnum.PL_APPENDIX_2320, PlAppendix23202355DTO.class, LedgerSheetCode.PL_APPENDIX_2320);
        MonthlySettlementTaxParsedDTO monthly = SheetDataReaders.requireObjectPreloaded(
                ctx, FileCategoryEnum.MONTHLY_SETTLEMENT_TAX, MonthlySettlementTaxParsedDTO.class, LedgerSheetCode.PL_APPENDIX_2320);

        PlAppendix23202355DTO working = deepCopy(source);
        List<PlAppendix23202355DTO.InvoicingSplitItem> section1 = working.getInvoicingSplitList() == null
                ? new ArrayList<>()
                : new ArrayList<>(working.getInvoicingSplitList());
        List<PlAppendix23202355DTO.DeclarationSplitItem> section2 = working.getDeclarationSplitList() == null
                ? new ArrayList<>()
                : new ArrayList<>(working.getDeclarationSplitList());

        cleanRows(section1, section2);

        Map<String, PlAppendix23202355DTO.InvoicingSplitItem> section1ByKey = new LinkedHashMap<>();
        Map<String, PlAppendix23202355DTO.DeclarationSplitItem> section2ByKey = new LinkedHashMap<>();
        for (PlAppendix23202355DTO.InvoicingSplitItem row : section1) {
            String key = splitBasisKey(row.getSplitBasis(), "Section1");
            if (section1ByKey.putIfAbsent(key, row) != null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "PL附表-2320、2355：Section1拆分依据重复: " + row.getSplitBasis());
            }
        }
        for (PlAppendix23202355DTO.DeclarationSplitItem row : section2) {
            String key = splitBasisKey(row.getSplitBasis(), "Section2");
            if (section2ByKey.putIfAbsent(key, row) != null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "PL附表-2320、2355：Section2拆分依据重复: " + row.getSplitBasis());
            }
        }
        ensureSameKeys(section1ByKey.keySet(), section2ByKey.keySet());

        for (Map.Entry<String, PlAppendix23202355DTO.DeclarationSplitItem> entry : section2ByKey.entrySet()) {
            String key = entry.getKey();
            if (!isSpecialInvoiceKey(key)) {
                continue;
            }
            String rate = key.substring("专票-".length());
            MonthlySettlementTaxParsedDTO.RateAggregate agg = findRateAggregate(monthly, rate);
            entry.getValue().setDeclaredAmount(zeroIfNull(agg == null ? null : agg.getIncomeSum()));
            entry.getValue().setDeclaredTaxAmount(zeroIfNull(agg == null ? null : agg.getOutputTaxSum()));
        }

        for (Map.Entry<String, PlAppendix23202355DTO.InvoicingSplitItem> entry : section1ByKey.entrySet()) {
            String key = entry.getKey();
            if (!isSpecialInvoiceKey(key)) {
                continue;
            }
            String rate = key.substring("专票-".length());
            MonthlySettlementTaxParsedDTO.RateAggregate agg = findRateAggregate(monthly, rate);

            BigDecimal invoicedIncome = zeroIfNull(agg == null ? null : agg.getInvoicedIncomeSum());
            BigDecimal invoicedOutputTax = zeroIfNull(agg == null ? null : agg.getInvoicedTaxAmountSum());
            PlAppendix23202355DTO.DeclarationSplitItem declaration = section2ByKey.get(key);
            BigDecimal declaredAmount = zeroIfNull(declaration == null ? null : declaration.getDeclaredAmount());
            BigDecimal declaredTaxAmount = zeroIfNull(declaration == null ? null : declaration.getDeclaredTaxAmount());

            PlAppendix23202355DTO.InvoicingSplitItem invoicing = entry.getValue();
            invoicing.setInvoicedIncome(invoicedIncome);
            invoicing.setInvoicedOutputTax(invoicedOutputTax);
            invoicing.setUninvoicedIncome(declaredAmount.subtract(invoicedIncome));
            invoicing.setOutputTax(declaredTaxAmount.subtract(invoicedOutputTax));
        }

        working.setInvoicingSplitList(section1);
        working.setDeclarationSplitList(section2);
        return new PlAppendix2320LedgerSheetData(working);
    }

    private PlAppendix23202355DTO deepCopy(PlAppendix23202355DTO source) {
        PlAppendix23202355DTO target = new PlAppendix23202355DTO();
        List<PlAppendix23202355DTO.InvoicingSplitItem> sec1 = new ArrayList<>();
        if (source.getInvoicingSplitList() != null) {
            for (PlAppendix23202355DTO.InvoicingSplitItem row : source.getInvoicingSplitList()) {
                if (row == null) {
                    continue;
                }
                PlAppendix23202355DTO.InvoicingSplitItem c = new PlAppendix23202355DTO.InvoicingSplitItem();
                c.setSplitBasis(row.getSplitBasis());
                c.setUninvoicedIncome(row.getUninvoicedIncome());
                c.setOutputTax(row.getOutputTax());
                c.setInvoicedIncome(row.getInvoicedIncome());
                c.setInvoicedOutputTax(row.getInvoicedOutputTax());
                sec1.add(c);
            }
        }
        List<PlAppendix23202355DTO.DeclarationSplitItem> sec2 = new ArrayList<>();
        if (source.getDeclarationSplitList() != null) {
            for (PlAppendix23202355DTO.DeclarationSplitItem row : source.getDeclarationSplitList()) {
                if (row == null) {
                    continue;
                }
                PlAppendix23202355DTO.DeclarationSplitItem c = new PlAppendix23202355DTO.DeclarationSplitItem();
                c.setSplitBasis(row.getSplitBasis());
                c.setDeclaredAmount(row.getDeclaredAmount());
                c.setDeclaredTaxAmount(row.getDeclaredTaxAmount());
                sec2.add(c);
            }
        }
        target.setInvoicingSplitList(sec1);
        target.setDeclarationSplitList(sec2);
        return target;
    }

    private void cleanRows(List<PlAppendix23202355DTO.InvoicingSplitItem> section1,
                           List<PlAppendix23202355DTO.DeclarationSplitItem> section2) {
        section1.removeIf(row -> isBlank(row == null ? null : row.getSplitBasis()));
        section2.removeIf(row -> isBlank(row == null ? null : row.getSplitBasis()));
        section1.removeIf(row -> row != null && isTotalRow(row.getSplitBasis()));
        section2.removeIf(row -> row != null && isTotalRow(row.getSplitBasis()));
        section1.removeIf(row -> row != null && isNormalInvoice(row.getSplitBasis())
                && allNull(row.getUninvoicedIncome(), row.getOutputTax(), row.getInvoicedIncome(), row.getInvoicedOutputTax()));
        section2.removeIf(row -> row != null && isNormalInvoice(row.getSplitBasis())
                && allNull(row.getDeclaredAmount(), row.getDeclaredTaxAmount()));
    }

    private void ensureSameKeys(Set<String> section1Keys, Set<String> section2Keys) {
        Set<String> s1 = new LinkedHashSet<>(section1Keys);
        Set<String> s2 = new LinkedHashSet<>(section2Keys);
        Set<String> only1 = new LinkedHashSet<>(s1);
        only1.removeAll(s2);
        Set<String> only2 = new LinkedHashSet<>(s2);
        only2.removeAll(s1);
        if (!only1.isEmpty() || !only2.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "PL附表-2320、2355：Section1 与 Section2 拆分依据不一致，"
                    + "Section2缺少=" + String.join(",", only1) + "，Section1缺少=" + String.join(",", only2));
        }
    }

    private String splitBasisKey(String splitBasis, String sectionName) {
        String text = normalize(splitBasis);
        if (isBlank(text)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "PL附表-2320、2355：" + sectionName + "拆分依据不能为空");
        }
        String rate = extractTaxRate(text);
        if (isBlank(rate)) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "PL附表-2320、2355：" + sectionName + "拆分依据无法提取税率: " + splitBasis);
        }
        return (isNormalInvoice(text) ? "普票-" : "专票-") + rate;
    }

    private MonthlySettlementTaxParsedDTO.RateAggregate findRateAggregate(MonthlySettlementTaxParsedDTO monthly, String rate) {
        if (monthly == null || monthly.getAggregateByRate() == null || isBlank(rate)) {
            return null;
        }
        MonthlySettlementTaxParsedDTO.RateAggregate aggregate = monthly.getAggregateByRate().get(rate);
        if (aggregate == null) {
            log.warn("PL附表-2320、2355构建：月结缺失税率聚合，rate={}", rate);
        }
        return aggregate;
    }

    private boolean isSpecialInvoiceKey(String key) {
        return key != null && key.startsWith("专票-");
    }

    private boolean isNormalInvoice(String splitBasis) {
        String text = normalize(splitBasis);
        return text != null && text.contains("普票");
    }

    private boolean isTotalRow(String splitBasis) {
        String text = normalize(splitBasis);
        return text != null && text.contains("合计");
    }

    private String extractTaxRate(String text) {
        Matcher matcher = TAX_RATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        try {
            return new BigDecimal(matcher.group(1)).stripTrailingZeros().toPlainString() + "%";
        } catch (NumberFormatException ex) {
            return matcher.group(1) + "%";
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.replace("\u00A0", " ").trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private boolean allNull(BigDecimal... values) {
        for (BigDecimal value : values) {
            if (value != null) {
                return false;
            }
        }
        return true;
    }
}
