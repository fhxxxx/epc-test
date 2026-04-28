package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.ContractStampDutyLedgerItemDTO;
import com.envision.epc.module.taxledger.application.dto.StampDutyDetailRowDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.StampTaxProjectLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * 印花税明细--非2320、2355 页数据构建器。
 */
@Component
public class StampTaxProjectSheetDataBuilder implements LedgerSheetDataBuilder<StampTaxProjectLedgerSheetData> {
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.STAMP_TAX_PROJECT;
    }

    @Override
    public StampTaxProjectLedgerSheetData build(LedgerBuildContext ctx) {
        List<ContractStampDutyLedgerItemDTO> sourceRows = SheetDataReaders.requireList(
                ctx,
                FileCategoryEnum.CONTRACT_STAMP_DUTY_LEDGER,
                ContractStampDutyLedgerItemDTO.class,
                LedgerSheetCode.STAMP_TAX_PROJECT
        );
        String targetQuarter = quarterOf(ctx.getYearMonth());

        Map<String, TaxAccumulator> grouped = new LinkedHashMap<>();
        for (ContractStampDutyLedgerItemDTO row : sourceRows) {
            if (row == null) {
                continue;
            }
            if (!targetQuarter.equals(normalizeQuarter(row.getQuarter()))) {
                continue;
            }
            String taxItem = normalizeText(row.getStampDutyTaxItem());
            if (taxItem == null) {
                continue;
            }
            TaxAccumulator acc = grouped.computeIfAbsent(taxItem, key -> new TaxAccumulator());
            acc.taxItem = taxItem;
            acc.taxableBasis = addNullable(acc.taxableBasis, row.getContractAmount());
            if (acc.taxRate == null && row.getStampDutyTaxRate() != null) {
                acc.taxRate = row.getStampDutyTaxRate();
            }
            acc.taxPayableAmount = addNullable(acc.taxPayableAmount, row.getTaxableAmount());
            BigDecimal reduction = reductionAmount(row.getContractAmount(), row.getStampDutyTaxRate(), row.getPreferentialRatio());
            acc.taxReductionAmount = addNullable(acc.taxReductionAmount, reduction);
        }

        List<StampDutyDetailRowDTO> payload = new ArrayList<>();
        int seq = 1;
        for (TaxAccumulator acc : grouped.values()) {
            StampDutyDetailRowDTO dto = new StampDutyDetailRowDTO();
            dto.setSerialNo(String.valueOf(seq++));
            dto.setTaxItem(acc.taxItem);
            dto.setTaxableBasis(acc.taxableBasis);
            dto.setTaxRate(acc.taxRate);
            dto.setTaxPayableAmount(acc.taxPayableAmount);
            dto.setTaxReductionAmount(acc.taxReductionAmount);
            dto.setTaxPaidAmount(null);
            dto.setTaxPayableOrRefundableAmount(subtractNullable(acc.taxPayableAmount, acc.taxReductionAmount));
            payload.add(dto);
        }

        StampDutyDetailRowDTO total = buildTotalRow(payload);
        payload.add(total);

        String companyName = null;
        if (ctx.getConfigSnapshot() != null && ctx.getConfigSnapshot().getCurrentCompany() != null) {
            companyName = normalizeText(ctx.getConfigSnapshot().getCurrentCompany().getCompanyName());
        }
        return new StampTaxProjectLedgerSheetData(companyName == null ? "" : companyName, payload);
    }

    private StampDutyDetailRowDTO buildTotalRow(List<StampDutyDetailRowDTO> detailRows) {
        BigDecimal totalTaxableBasis = BigDecimal.ZERO;
        BigDecimal totalTaxPayableAmount = BigDecimal.ZERO;
        BigDecimal totalTaxReductionAmount = BigDecimal.ZERO;
        BigDecimal totalTaxPayableOrRefundableAmount = BigDecimal.ZERO;
        for (StampDutyDetailRowDTO row : detailRows) {
            totalTaxableBasis = totalTaxableBasis.add(nvl(row.getTaxableBasis()));
            totalTaxPayableAmount = totalTaxPayableAmount.add(nvl(row.getTaxPayableAmount()));
            totalTaxReductionAmount = totalTaxReductionAmount.add(nvl(row.getTaxReductionAmount()));
            totalTaxPayableOrRefundableAmount = totalTaxPayableOrRefundableAmount.add(nvl(row.getTaxPayableOrRefundableAmount()));
        }
        StampDutyDetailRowDTO total = new StampDutyDetailRowDTO();
        total.setSerialNo("合计");
        total.setTaxItem("");
        total.setTaxableBasis(totalTaxableBasis);
        total.setTaxRate(null);
        total.setTaxPayableAmount(totalTaxPayableAmount);
        total.setTaxReductionAmount(totalTaxReductionAmount);
        total.setTaxPaidAmount(null);
        total.setTaxPayableOrRefundableAmount(totalTaxPayableOrRefundableAmount);
        return total;
    }

    private String quarterOf(String yearMonth) {
        String normalized = normalizeYearMonth(yearMonth);
        int month = YearMonth.parse(normalized).getMonthValue();
        int quarter = (month - 1) / 3 + 1;
        return "Q" + quarter;
    }

    private String normalizeYearMonth(String yearMonth) {
        if (yearMonth == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 印花税明细--非2320、2355，yearMonth为空");
        }
        String normalized = yearMonth.trim();
        if (normalized.matches("^\\d{6}$")) {
            normalized = normalized.substring(0, 4) + "-" + normalized.substring(4, 6);
        }
        if (!normalized.matches("^\\d{4}-\\d{2}$")) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 印花税明细--非2320、2355，yearMonth格式非法: " + yearMonth);
        }
        return YearMonth.parse(normalized).toString();
    }

    private String normalizeQuarter(String quarter) {
        String text = normalizeText(quarter);
        if (text == null) {
            return "";
        }
        return text.toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        return text.isEmpty() ? null : text;
    }

    private BigDecimal reductionAmount(BigDecimal contractAmount, BigDecimal stampDutyTaxRate, BigDecimal preferentialRatio) {
        if (contractAmount == null || stampDutyTaxRate == null || preferentialRatio == null) {
            return null;
        }
        return contractAmount.multiply(stampDutyTaxRate).multiply(preferentialRatio);
    }

    private BigDecimal addNullable(BigDecimal left, BigDecimal right) {
        if (right == null) {
            return left;
        }
        if (left == null) {
            return right;
        }
        return left.add(right);
    }

    private BigDecimal subtractNullable(BigDecimal left, BigDecimal right) {
        if (left == null && right == null) {
            return null;
        }
        return nvl(left).subtract(nvl(right));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class TaxAccumulator {
        private String taxItem;
        private BigDecimal taxableBasis;
        private BigDecimal taxRate;
        private BigDecimal taxPayableAmount;
        private BigDecimal taxReductionAmount;
    }
}

