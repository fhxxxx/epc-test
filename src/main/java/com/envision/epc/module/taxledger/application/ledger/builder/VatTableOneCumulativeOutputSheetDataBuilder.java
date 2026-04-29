package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.facade.azure.BlobStorageRemote;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.data.VatTableOneCumulativeOutputLedgerSheetData;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SheetParseService;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.application.dto.VatTableOneCumulativeOutputItemDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * 增值税表一 累计销项-2320、2355 页数据构建器。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VatTableOneCumulativeOutputSheetDataBuilder implements LedgerSheetDataBuilder<VatTableOneCumulativeOutputLedgerSheetData> {
    private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]");

    private final PreviousLedgerLocator previousLedgerLocator;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_TABLE_ONE_CUMULATIVE_OUTPUT;
    }

    @Override
    public VatTableOneCumulativeOutputLedgerSheetData build(LedgerBuildContext ctx) {
        ensureCompanyScope(ctx.getCompanyCode());

        String normalizedYm = PreviousLedgerLocator.normalizeYearMonth(ctx.getYearMonth());
        String currentPeriod = normalizedYm.replace("-", "");

        List<VatTableOneCumulativeOutputItemDTO> previousRows = parsePreviousRows(ctx, normalizedYm);
        if (previousRows.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 增值税表一 累计销项-2320、2355，缺少前序台账，请先上传初始台账");
        }

        LinkedHashSet<String> taxCategorySet = new LinkedHashSet<>();
        for (VatTableOneCumulativeOutputItemDTO row : previousRows) {
            String category = trim(row == null ? null : row.getTaxCategory());
            if (!isBlank(category)) {
                taxCategorySet.add(category);
            }
        }
        if (taxCategorySet.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 增值税表一 累计销项-2320、2355，前序台账缺少涉税类别");
        }

        VatChangeLedgerSheetData vatChangeData =
                ctx.requireBuilt(LedgerSheetCode.VAT_CHANGE, VatChangeLedgerSheetData.class, support());
        List<VatChangeRowDTO> vatChangeRows = vatChangeData.getPayload() == null ? List.of() : vatChangeData.getPayload();

        List<VatTableOneCumulativeOutputItemDTO> result = new ArrayList<>(previousRows.size() + taxCategorySet.size());
        result.addAll(copyRows(previousRows));
        for (String taxCategory : taxCategorySet) {
            BigDecimal rate = extractRateDecimalOrThrow(taxCategory);

            BigDecimal invoicedAmount = sumVatChangeByBaseAndRate(vatChangeRows, "当月利润表主营业务收入", rate, true);
            BigDecimal uninvoicedAmount = sumVatChangeByBaseAndRate(vatChangeRows, "当月利润表主营业务收入", rate, false);
            BigDecimal invoicedTax = sumVatChangeByBaseAndRate(vatChangeRows, "应交增值税-销项税", rate, true);
            BigDecimal uninvoicedTax = sumVatChangeByBaseAndRate(vatChangeRows, "应交增值税-销项税", rate, false);

            warnIfMissing(vatChangeRows, taxCategory, rate, "当月利润表主营业务收入");
            warnIfMissing(vatChangeRows, taxCategory, rate, "应交增值税-销项税");

            VatTableOneCumulativeOutputItemDTO item = new VatTableOneCumulativeOutputItemDTO();
            item.setPeriod(currentPeriod);
            item.setTaxCategory(taxCategory);
            item.setInvoicedAmountExclTax(invoicedAmount);
            item.setUninvoicedAmountExclTax(uninvoicedAmount);
            item.setInvoicedTaxAmount(invoicedTax);
            item.setUninvoicedTaxAmount(uninvoicedTax);
            item.setInvoicedTaxRate(rate);
            item.setUninvoicedTaxRate(rate);
            item.setTotalAmountExclTax(nvl(invoicedAmount).add(nvl(uninvoicedAmount)));
            item.setTotalTaxAmount(nvl(invoicedTax).add(nvl(uninvoicedTax)));
            result.add(item);
        }

        return new VatTableOneCumulativeOutputLedgerSheetData(result);
    }

    private void ensureCompanyScope(String companyCode) {
        if (!"2320".equals(companyCode) && !"2355".equals(companyCode)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 增值税表一 累计销项-2320、2355，仅支持2320/2355公司");
        }
    }

    private List<VatTableOneCumulativeOutputItemDTO> parsePreviousRows(LedgerBuildContext ctx, String currentYearMonth) {
        PreviousLedgerLocator.PreviousLedgerRef previous = previousLedgerLocator.find(ctx.getCompanyCode(), currentYearMonth);
        if (previous == null || isBlank(previous.ledgerBlobPath())) {
            return List.of();
        }
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(previous.ledgerBlobPath(), out);
            ParseResult<?> parsed = sheetParseService.parse(
                    new ByteArrayInputStream(out.toByteArray()),
                    FileCategoryEnum.VAT_TABLE_ONE_CUMULATIVE_OUTPUT,
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
            List<?> rawList = (List<?>) parsed.getData();
            List<VatTableOneCumulativeOutputItemDTO> rows = new ArrayList<>();
            for (Object item : rawList) {
                if (item instanceof VatTableOneCumulativeOutputItemDTO) {
                    rows.add((VatTableOneCumulativeOutputItemDTO) item);
                }
            }
            return rows;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 增值税表一 累计销项-2320、2355，读取前序台账失败: " + ex.getMessage());
        }
    }

    private List<VatTableOneCumulativeOutputItemDTO> copyRows(List<VatTableOneCumulativeOutputItemDTO> source) {
        List<VatTableOneCumulativeOutputItemDTO> copied = new ArrayList<>(source.size());
        for (VatTableOneCumulativeOutputItemDTO row : source) {
            if (row == null) {
                continue;
            }
            VatTableOneCumulativeOutputItemDTO dto = new VatTableOneCumulativeOutputItemDTO();
            dto.setPeriod(row.getPeriod());
            dto.setTaxCategory(row.getTaxCategory());
            dto.setInvoicedAmountExclTax(row.getInvoicedAmountExclTax());
            dto.setInvoicedTaxAmount(row.getInvoicedTaxAmount());
            dto.setInvoicedTaxRate(row.getInvoicedTaxRate());
            dto.setUninvoicedAmountExclTax(row.getUninvoicedAmountExclTax());
            dto.setUninvoicedTaxAmount(row.getUninvoicedTaxAmount());
            dto.setUninvoicedTaxRate(row.getUninvoicedTaxRate());
            dto.setTotalAmountExclTax(row.getTotalAmountExclTax());
            dto.setTotalTaxAmount(row.getTotalTaxAmount());
            copied.add(dto);
        }
        return copied;
    }

    private BigDecimal extractRateDecimalOrThrow(String taxCategory) {
        Matcher matcher = RATE_PATTERN.matcher(taxCategory);
        if (!matcher.find()) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "组装Sheet数据失败: 增值税表一 累计销项-2320、2355，涉税类别无法提取税率: " + taxCategory);
        }
        String number = matcher.group(1);
        return new BigDecimal(number).divide(BigDecimal.valueOf(100));
    }

    private BigDecimal sumVatChangeByBaseAndRate(List<VatChangeRowDTO> rows, String baseItem, BigDecimal rate, boolean invoicedMetric) {
        String normalizedBase = normalizeBase(baseItem);
        BigDecimal sum = BigDecimal.ZERO;
        for (VatChangeRowDTO row : rows) {
            if (row == null || !Objects.equals(normalizedBase, normalizeBase(row.getBaseItem()))) {
                continue;
            }
            BigDecimal rowRate = extractRateDecimal(trim(row.getSplitBasis()));
            if (rowRate == null || rowRate.compareTo(rate) != 0) {
                continue;
            }
            sum = sum.add(nvl(invoicedMetric ? row.getCurrentMonthInvoicedAmount() : row.getUnbilledAmount()));
        }
        return sum;
    }

    private void warnIfMissing(List<VatChangeRowDTO> rows, String taxCategory, BigDecimal rate, String baseItem) {
        String normalizedBase = normalizeBase(baseItem);
        boolean matched = false;
        for (VatChangeRowDTO row : rows) {
            if (row == null || !Objects.equals(normalizedBase, normalizeBase(row.getBaseItem()))) {
                continue;
            }
            BigDecimal rowRate = extractRateDecimal(trim(row.getSplitBasis()));
            if (rowRate != null && rowRate.compareTo(rate) == 0) {
                matched = true;
                break;
            }
        }
        if (!matched) {
            log.warn("VAT_TABLE_ONE_CUMULATIVE_OUTPUT match missing, category={}, rate={}, baseItem={}, fallback=0",
                    taxCategory, rate, baseItem);
        }
    }

    private BigDecimal extractRateDecimal(String text) {
        if (isBlank(text)) {
            return null;
        }
        Matcher matcher = RATE_PATTERN.matcher(text);
        if (!matcher.find()) {
            return null;
        }
        return new BigDecimal(matcher.group(1)).divide(BigDecimal.valueOf(100));
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

    private String trim(String text) {
        return text == null ? null : text.trim();
    }

    private boolean isBlank(String text) {
        return text == null || text.trim().isEmpty();
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

