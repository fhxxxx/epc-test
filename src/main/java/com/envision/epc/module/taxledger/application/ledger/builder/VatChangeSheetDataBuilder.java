package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;
import com.envision.epc.module.taxledger.application.dto.BsStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.DlInputParsedDTO;
import com.envision.epc.module.taxledger.application.dto.MonthlySettlementTaxParsedDTO;
import com.envision.epc.module.taxledger.application.dto.MonthlyTaxSectionDTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.dto.PlAppendixProjectCompanyUploadDTO;
import com.envision.epc.module.taxledger.application.dto.PlStatementRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeAppendixUploadDTO;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.dto.VatInputCertParsedDTO;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.ledger.LedgerBuildContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetDataBuilder;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.domain.VatBasicItemConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 增值税变动表 页数据构建器。
 */
@Component
public class VatChangeSheetDataBuilder implements LedgerSheetDataBuilder<VatChangeLedgerSheetData> {
    private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]");
    private static final String ACCOUNT_INPUT_TRANSFER_OUT = "2221010400";
    private static final String INVOICE_TYPE_SPECIAL = "增值税专用发票";
    private static final String INVOICE_TYPE_GENERAL = "普通发票";

    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_CHANGE;
    }

    @Override
    public VatChangeLedgerSheetData build(LedgerBuildContext ctx) {
        boolean is2320Or2355 = isCompany2320Or2355(ctx.getCompanyCode());

        VatChangeAppendixUploadDTO vatChangeAppendix = SheetDataReaders.requireObject(
                ctx, FileCategoryEnum.VAT_CHANGE_APPENDIX, VatChangeAppendixUploadDTO.class, LedgerSheetCode.VAT_CHANGE);
        VatOutputSheetUploadDTO vatOutput = SheetDataReaders.requireObject(
                ctx, FileCategoryEnum.VAT_OUTPUT, VatOutputSheetUploadDTO.class, LedgerSheetCode.VAT_CHANGE);
        VatInputCertParsedDTO vatInputCert = SheetDataReaders.requireObject(
                ctx, FileCategoryEnum.VAT_INPUT_CERT, VatInputCertParsedDTO.class, LedgerSheetCode.VAT_CHANGE);
        DlInputParsedDTO dlInput = SheetDataReaders.requireObject(
                ctx, FileCategoryEnum.DL_INPUT, DlInputParsedDTO.class, LedgerSheetCode.VAT_CHANGE);
        List<PlStatementRowDTO> plRows = SheetDataReaders.requireList(
                ctx, FileCategoryEnum.PL, PlStatementRowDTO.class, LedgerSheetCode.VAT_CHANGE);
        List<BsStatementRowDTO> bsRows = SheetDataReaders.requireList(
                ctx, FileCategoryEnum.BS, BsStatementRowDTO.class, LedgerSheetCode.VAT_CHANGE);

        PlAppendix23202355DTO plAppendix2320 = null;
        MonthlySettlementTaxParsedDTO monthlySettlementTax = null;
        List<PlAppendixProjectCompanyUploadDTO> plAppendixProject = List.of();
        if (is2320Or2355) {
            plAppendix2320 = SheetDataReaders.requireObject(
                    ctx, FileCategoryEnum.PL_APPENDIX_2320, PlAppendix23202355DTO.class, LedgerSheetCode.VAT_CHANGE);
            monthlySettlementTax = SheetDataReaders.requireObject(
                    ctx, FileCategoryEnum.MONTHLY_SETTLEMENT_TAX, MonthlySettlementTaxParsedDTO.class, LedgerSheetCode.VAT_CHANGE);
        } else {
            plAppendixProject = SheetDataReaders.requireList(
                    ctx, FileCategoryEnum.PL_APPENDIX_PROJECT, PlAppendixProjectCompanyUploadDTO.class, LedgerSheetCode.VAT_CHANGE);
        }

        List<VatChangeRowDTO> rows = buildStructureRows(ctx, is2320Or2355, plAppendix2320, plAppendixProject);
        fillTotalAmount(rows, is2320Or2355, plAppendix2320, plAppendixProject, plRows, bsRows, vatInputCert, vatChangeAppendix, dlInput);
        fillCurrentMonthInvoicedAmount(rows, is2320Or2355, vatOutput, monthlySettlementTax);
        fillPreviousMonthInvoicedAmount(rows);
        fillUnbilledAmount(rows);

        String appendixBlobPath = findLatestBlobPath(ctx, FileCategoryEnum.VAT_CHANGE_APPENDIX);
        return new VatChangeLedgerSheetData(appendixBlobPath, null, rows);
    }

    private List<VatChangeRowDTO> buildStructureRows(LedgerBuildContext ctx,
                                                     boolean is2320Or2355,
                                                     PlAppendix23202355DTO plAppendix2320,
                                                     List<PlAppendixProjectCompanyUploadDTO> plAppendixProject) {
        List<VatBasicItemConfig> configs = ctx.getConfigSnapshot() == null ? List.of() : ctx.getConfigSnapshot().getVatBasicItemConfigs();
        if (configs == null || configs.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 增值税变动表，缺少基础条目配置");
        }
        List<VatBasicItemConfig> sorted = configs.stream()
                .filter(Objects::nonNull)
                .filter(cfg -> cfg.getIsDeleted() == null || cfg.getIsDeleted() == 0)
                .filter(cfg -> "Y".equalsIgnoreCase(trim(cfg.getIsDisplay())))
                .filter(cfg -> isBlank(cfg.getCompanyCode()) || Objects.equals(trim(cfg.getCompanyCode()), trim(ctx.getCompanyCode())))
                .sorted(Comparator.comparing(cfg -> cfg.getItemSeq() == null ? Integer.MAX_VALUE : cfg.getItemSeq()))
                .toList();

        List<String> splitBasisSource = is2320Or2355
                ? extractSplitBasisFromPl2320(plAppendix2320)
                : extractSplitBasisFromPlProject(plAppendixProject);

        List<VatChangeRowDTO> rows = new ArrayList<>();
        for (VatBasicItemConfig cfg : sorted) {
            String baseItem = trim(cfg.getBasicItem());
            if (isBlank(baseItem)) {
                continue;
            }
            boolean split = "Y".equalsIgnoreCase(trim(cfg.getIsSplit()));
            if (!split) {
                rows.add(newRow(baseItem, null));
                continue;
            }
            for (String splitBasis : splitBasisSource) {
                if (isBlank(splitBasis)) {
                    continue;
                }
                rows.add(newRow(baseItem, splitBasis));
            }
        }
        return rows;
    }

    private void fillTotalAmount(List<VatChangeRowDTO> rows,
                                 boolean is2320Or2355,
                                 PlAppendix23202355DTO plAppendix2320,
                                 List<PlAppendixProjectCompanyUploadDTO> plAppendixProject,
                                 List<PlStatementRowDTO> plRows,
                                 List<BsStatementRowDTO> bsRows,
                                 VatInputCertParsedDTO vatInputCert,
                                 VatChangeAppendixUploadDTO vatChangeAppendix,
                                 DlInputParsedDTO dlInput) {
        for (VatChangeRowDTO row : rows) {
            String base = normalizeBaseItem(row.getBaseItem());
            String splitBasis = trim(row.getSplitBasis());

            if (isMainBusinessRevenue(base)) {
                if (is2320Or2355) {
                    row.setTotalAmount(findDeclaredAmountBySplitBasis(plAppendix2320, splitBasis));
                } else {
                    BigDecimal fromAppendix = findProjectAppendixMainBusinessRevenue(plAppendixProject, splitBasis);
                    row.setTotalAmount(fromAppendix != null ? fromAppendix : findPlMainBusinessRevenue(plRows));
                }
                continue;
            }

            if (isOutputTaxPayable(base)) {
                if (is2320Or2355) {
                    row.setTotalAmount(findDeclaredTaxAmountBySplitBasis(plAppendix2320, splitBasis));
                } else {
                    BigDecimal fromAppendix = findProjectAppendixOutputTax(plAppendixProject, splitBasis);
                    row.setTotalAmount(fromAppendix != null ? fromAppendix : calculateBsTaxPayableFallback(bsRows));
                }
                continue;
            }

            if (isCertifiedInputTax(base)) {
                row.setTotalAmount(nvl(vatInputCert == null ? null : vatInputCert.getTaxAmountSum()));
                continue;
            }
            if (isOpeningRetainedInputTax(base)) {
                row.setTotalAmount(resolveOpeningRetainedInputTax());
                continue;
            }
            if (isPrepaidDeduction(base)) {
                row.setTotalAmount(nvl(vatChangeAppendix == null ? null : vatChangeAppendix.getCurrentPeriodPrepaidDeduction()));
                continue;
            }
            if (isInputTransferOut(base)) {
                BigDecimal value = dlInput == null || dlInput.getLocalAmountSumByAccount() == null
                        ? BigDecimal.ZERO
                        : nvl(dlInput.getLocalAmountSumByAccount().get(ACCOUNT_INPUT_TRANSFER_OUT));
                row.setTotalAmount(value);
            }
        }

        for (VatChangeRowDTO row : rows) {
            String base = normalizeBaseItem(row.getBaseItem());
            if (isEndingRetainedInputTax(base)) {
                BigDecimal x = sumByBaseItem(rows, "应交增值税-销项税")
                        .subtract(sumByBaseItem(rows, "增值税已认证进项税")
                                .add(sumByBaseItem(rows, "期初留抵进项税"))
                                .subtract(sumByBaseItem(rows, "进项转出")));
                row.setTotalAmount(x.signum() < 0 ? x.abs() : BigDecimal.ZERO);
            }
        }
        for (VatChangeRowDTO row : rows) {
            String base = normalizeBaseItem(row.getBaseItem());
            if (isVatPayable(base)) {
                BigDecimal endingRetained = sumByBaseItem(rows, "期末留抵进项税");
                if (endingRetained.signum() > 0) {
                    row.setTotalAmount(BigDecimal.ZERO);
                } else {
                    BigDecimal result = sumByBaseItem(rows, "销项税额")
                            .add(sumByBaseItem(rows, "应交增值税-销项税"))
                            .add(sumByBaseItem(rows, "进项转出"))
                            .subtract(sumByBaseItem(rows, "增值税已认证进项税"))
                            .subtract(sumByBaseItem(rows, "期初留抵进项税"));
                    row.setTotalAmount(result);
                }
            }
        }
        for (VatChangeRowDTO row : rows) {
            String base = normalizeBaseItem(row.getBaseItem());
            if (isCurrentPeriodVatPayable(base)) {
                row.setTotalAmount(sumByBaseItem(rows, "应交增值税")
                        .subtract(sumByBaseItem(rows, "异地预缴递减")));
            }
        }
    }

    private void fillCurrentMonthInvoicedAmount(List<VatChangeRowDTO> rows,
                                                boolean is2320Or2355,
                                                VatOutputSheetUploadDTO vatOutput,
                                                MonthlySettlementTaxParsedDTO monthlySettlementTax) {
        for (VatChangeRowDTO row : rows) {
            String base = normalizeBaseItem(row.getBaseItem());
            if (!isMainBusinessRevenue(base) && !isOutputTaxPayable(base)) {
                continue;
            }
            if (is2320Or2355) {
                String splitBasis = trim(row.getSplitBasis());
                String rateText = extractRateText(splitBasis);
                if (isBlank(rateText)) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "增值税变动表：拆分依据无法提取税率，baseItem="
                            + row.getBaseItem() + ", splitBasis=" + splitBasis);
                }
                BigDecimal rate = rateTextToDecimal(rateText);
                boolean general = containsIgnoreCase(splitBasis, "普票");
                if (isMainBusinessRevenue(base)) {
                    if (general) {
                        row.setCurrentMonthInvoicedAmount(
                                findVatOutputValue(vatOutput, rate, INVOICE_TYPE_GENERAL, true));
                    } else {
                        BigDecimal vatValue = findVatOutputValue(vatOutput, rate, INVOICE_TYPE_SPECIAL, true);
                        BigDecimal monthlyValue = sumMonthlyByRate(monthlySettlementTax, rateText, true);
                        ensureAmountEquals(vatValue, monthlyValue,
                                "当月利润表主营业务收入*的当月开票金额【增值税销项表-按税率（征收率）统计表】与【睿景景程月结数据表-报税】不一致，请检查。");
                        row.setCurrentMonthInvoicedAmount(vatValue);
                    }
                } else {
                    if (general) {
                        row.setCurrentMonthInvoicedAmount(
                                findVatOutputValue(vatOutput, rate, INVOICE_TYPE_GENERAL, false));
                    } else {
                        BigDecimal vatValue = findVatOutputValue(vatOutput, rate, INVOICE_TYPE_SPECIAL, false);
                        BigDecimal monthlyValue = sumMonthlyByRate(monthlySettlementTax, rateText, false);
                        ensureAmountEquals(vatValue, monthlyValue,
                                "应交增值税-销项税*的当月开票金额【增值税销项表-按税率（征收率）统计表】与【睿景景程月结数据表-报税】不一致，请检查。");
                        row.setCurrentMonthInvoicedAmount(vatValue);
                    }
                }
            } else {
                if (isMainBusinessRevenue(base)) {
                    row.setCurrentMonthInvoicedAmount(sumVatOutputTotal(vatOutput, true));
                } else if (isOutputTaxPayable(base)) {
                    row.setCurrentMonthInvoicedAmount(sumVatOutputTotal(vatOutput, false));
                }
            }
        }
    }

    private void fillPreviousMonthInvoicedAmount(List<VatChangeRowDTO> rows) {
        for (VatChangeRowDTO row : rows) {
            row.setPreviousMonthInvoicedAmount(null);
        }
    }

    private void fillUnbilledAmount(List<VatChangeRowDTO> rows) {
        for (VatChangeRowDTO row : rows) {
            String base = normalizeBaseItem(row.getBaseItem());
            if (!isMainBusinessRevenue(base) && !isOutputTaxPayable(base)) {
                row.setUnbilledAmount(null);
                continue;
            }
            BigDecimal unbilled = nvl(row.getTotalAmount())
                    .subtract(nvl(row.getCurrentMonthInvoicedAmount()))
                    .subtract(nvl(row.getPreviousMonthInvoicedAmount()));
            row.setUnbilledAmount(unbilled);
        }
    }

    private VatChangeRowDTO newRow(String baseItem, String splitBasis) {
        VatChangeRowDTO row = new VatChangeRowDTO();
        row.setBaseItem(baseItem);
        row.setSplitBasis(splitBasis);
        row.setItemName(isBlank(splitBasis) ? baseItem : baseItem + "-" + splitBasis);
        return row;
    }

    private List<String> extractSplitBasisFromPl2320(PlAppendix23202355DTO dto) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (dto == null) {
            return List.of();
        }
        if (dto.getDeclarationSplitList() != null) {
            for (PlAppendix23202355DTO.DeclarationSplitItem row : dto.getDeclarationSplitList()) {
                String splitBasis = trim(row == null ? null : row.getSplitBasis());
                if (!isBlank(splitBasis) && !isTotalRow(splitBasis)) {
                    ordered.add(splitBasis);
                }
            }
        }
        if (dto.getInvoicingSplitList() != null) {
            for (PlAppendix23202355DTO.InvoicingSplitItem row : dto.getInvoicingSplitList()) {
                String splitBasis = trim(row == null ? null : row.getSplitBasis());
                if (!isBlank(splitBasis) && !isTotalRow(splitBasis)) {
                    ordered.add(splitBasis);
                }
            }
        }
        return new ArrayList<>(ordered);
    }

    private List<String> extractSplitBasisFromPlProject(List<PlAppendixProjectCompanyUploadDTO> rows) {
        LinkedHashSet<String> ordered = new LinkedHashSet<>();
        if (rows == null) {
            return List.of();
        }
        for (PlAppendixProjectCompanyUploadDTO row : rows) {
            String splitBasis = trim(row == null ? null : row.getSplitBasis());
            if (!isBlank(splitBasis)) {
                ordered.add(splitBasis);
            }
        }
        return new ArrayList<>(ordered);
    }

    private BigDecimal findDeclaredAmountBySplitBasis(PlAppendix23202355DTO dto, String splitBasis) {
        if (dto == null || dto.getDeclarationSplitList() == null) {
            return null;
        }
        PlAppendix23202355DTO.DeclarationSplitItem matched = findDeclarationRow(dto.getDeclarationSplitList(), splitBasis);
        return matched == null ? null : matched.getDeclaredAmount();
    }

    private BigDecimal findDeclaredTaxAmountBySplitBasis(PlAppendix23202355DTO dto, String splitBasis) {
        if (dto == null || dto.getDeclarationSplitList() == null) {
            return null;
        }
        PlAppendix23202355DTO.DeclarationSplitItem matched = findDeclarationRow(dto.getDeclarationSplitList(), splitBasis);
        return matched == null ? null : matched.getDeclaredTaxAmount();
    }

    private PlAppendix23202355DTO.DeclarationSplitItem findDeclarationRow(List<PlAppendix23202355DTO.DeclarationSplitItem> rows,
                                                                           String splitBasis) {
        String target = trim(splitBasis);
        if (isBlank(target)) {
            return null;
        }
        for (PlAppendix23202355DTO.DeclarationSplitItem row : rows) {
            if (row == null || isBlank(row.getSplitBasis())) {
                continue;
            }
            if (target.equals(trim(row.getSplitBasis()))) {
                return row;
            }
        }
        for (PlAppendix23202355DTO.DeclarationSplitItem row : rows) {
            if (row == null || isBlank(row.getSplitBasis())) {
                continue;
            }
            String source = trim(row.getSplitBasis());
            if (source.contains(target) || target.contains(source)) {
                return row;
            }
        }
        return null;
    }

    private BigDecimal findProjectAppendixMainBusinessRevenue(List<PlAppendixProjectCompanyUploadDTO> rows, String splitBasis) {
        PlAppendixProjectCompanyUploadDTO row = findProjectAppendixRow(rows, splitBasis);
        return row == null ? null : row.getMainBusinessRevenue();
    }

    private BigDecimal findProjectAppendixOutputTax(List<PlAppendixProjectCompanyUploadDTO> rows, String splitBasis) {
        PlAppendixProjectCompanyUploadDTO row = findProjectAppendixRow(rows, splitBasis);
        return row == null ? null : row.getOutputTax();
    }

    private PlAppendixProjectCompanyUploadDTO findProjectAppendixRow(List<PlAppendixProjectCompanyUploadDTO> rows, String splitBasis) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        String target = trim(splitBasis);
        if (isBlank(target)) {
            return null;
        }
        for (PlAppendixProjectCompanyUploadDTO row : rows) {
            if (row == null || isBlank(row.getSplitBasis())) {
                continue;
            }
            if (target.equals(trim(row.getSplitBasis()))) {
                return row;
            }
        }
        for (PlAppendixProjectCompanyUploadDTO row : rows) {
            if (row == null || isBlank(row.getSplitBasis())) {
                continue;
            }
            String source = trim(row.getSplitBasis());
            if (source.contains(target) || target.contains(source)) {
                return row;
            }
        }
        return null;
    }

    private BigDecimal findPlMainBusinessRevenue(List<PlStatementRowDTO> rows) {
        if (rows == null) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (PlStatementRowDTO row : rows) {
            if (row == null || isBlank(row.getItemName())) {
                continue;
            }
            if (containsIgnoreCase(row.getItemName(), "主营业务收入")) {
                sum = sum.add(nvl(row.getCurrentPeriodAmount()));
            }
        }
        return sum;
    }

    private BigDecimal calculateBsTaxPayableFallback(List<BsStatementRowDTO> rows) {
        if (rows == null) {
            return null;
        }
        for (BsStatementRowDTO row : rows) {
            if (row == null || isBlank(row.getItemName())) {
                continue;
            }
            if (containsIgnoreCase(row.getItemName(), "应交税费")) {
                return nvl(row.getAccumulatedAmount())
                        .subtract(nvl(row.getYearStartAmount()))
                        .multiply(BigDecimal.valueOf(-1));
            }
        }
        return null;
    }

    private BigDecimal findVatOutputValue(VatOutputSheetUploadDTO vatOutput,
                                          BigDecimal rate,
                                          String invoiceTypeKeyword,
                                          boolean amountMetric) {
        if (vatOutput == null || vatOutput.getTaxRateSummaries() == null) {
            return BigDecimal.ZERO;
        }
        for (VatOutputSheetUploadDTO.TaxRateSummaryItem row : vatOutput.getTaxRateSummaries()) {
            if (row == null) {
                continue;
            }
            if (row.getTaxRateOrLevyRate() == null || row.getTaxRateOrLevyRate().compareTo(rate) != 0) {
                continue;
            }
            if (!containsIgnoreCase(row.getInvoiceStatus(), invoiceTypeKeyword)) {
                continue;
            }
            return amountMetric ? nvl(row.getBlueInvoiceAmount()) : nvl(row.getBlueInvoiceTaxAmount());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal sumMonthlyByRate(MonthlySettlementTaxParsedDTO monthly, String rateText, boolean incomeMetric) {
        if (monthly == null || monthly.getSections() == null || isBlank(rateText)) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (MonthlyTaxSectionDTO row : monthly.getSections()) {
            if (row == null || Boolean.TRUE.equals(row.getTotalRow())) {
                continue;
            }
            if (!containsIgnoreCase(row.getTitle(), rateText)) {
                continue;
            }
            sum = sum.add(incomeMetric ? nvl(row.getInvoicedIncome()) : nvl(row.getInvoicedTaxAmount()));
        }
        return sum;
    }

    private BigDecimal sumVatOutputTotal(VatOutputSheetUploadDTO vatOutput, boolean amountMetric) {
        if (vatOutput == null || vatOutput.getTaxRateSummaries() == null || vatOutput.getTaxRateSummaries().isEmpty()) {
            return BigDecimal.ZERO;
        }
        for (VatOutputSheetUploadDTO.TaxRateSummaryItem row : vatOutput.getTaxRateSummaries()) {
            if (row == null) {
                continue;
            }
            if (!containsIgnoreCase(row.getInvoiceStatus(), "合计")) {
                continue;
            }
            return amountMetric ? nvl(row.getBlueInvoiceAmount()) : nvl(row.getBlueInvoiceTaxAmount());
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (VatOutputSheetUploadDTO.TaxRateSummaryItem row : vatOutput.getTaxRateSummaries()) {
            if (row == null) {
                continue;
            }
            sum = sum.add(amountMetric ? nvl(row.getBlueInvoiceAmount()) : nvl(row.getBlueInvoiceTaxAmount()));
        }
        return sum;
    }

    private void ensureAmountEquals(BigDecimal left, BigDecimal right, String message) {
        if (nvl(left).compareTo(nvl(right)) != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, message);
        }
    }

    private BigDecimal sumByBaseItem(List<VatChangeRowDTO> rows, String baseItem) {
        String normalizedTarget = normalizeBaseItem(baseItem);
        BigDecimal sum = BigDecimal.ZERO;
        for (VatChangeRowDTO row : rows) {
            if (row == null) {
                continue;
            }
            if (!normalizedTarget.equals(normalizeBaseItem(row.getBaseItem()))) {
                continue;
            }
            sum = sum.add(nvl(row.getTotalAmount()));
        }
        return sum;
    }

    private BigDecimal resolveOpeningRetainedInputTax() {
        // TODO: 从上期台账（台账月份-1）增值税变动表中读取“期末留抵进项税”合计金额。
        return BigDecimal.ZERO;
    }

    private String findLatestBlobPath(LedgerBuildContext ctx, FileCategoryEnum category) {
        if (ctx.getFiles() == null) {
            return null;
        }
        FileRecord latest = ctx.getFiles().stream()
                .filter(Objects::nonNull)
                .filter(file -> file.getIsDeleted() != null && file.getIsDeleted() == 0)
                .filter(file -> file.getFileCategory() == category)
                .max(Comparator.comparing(file -> file.getId() == null ? 0L : file.getId()))
                .orElse(null);
        if (latest == null || isBlank(latest.getBlobPath())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "组装Sheet数据失败: 增值税变动表，缺少增值税变动表附表源文件");
        }
        return latest.getBlobPath();
    }

    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }

    private boolean isMainBusinessRevenue(String normalizedBaseItem) {
        return "当月利润表主营业务收入".equals(normalizedBaseItem);
    }

    private boolean isOutputTaxPayable(String normalizedBaseItem) {
        return "应交增值税-销项税".equals(normalizedBaseItem);
    }

    private boolean isCertifiedInputTax(String normalizedBaseItem) {
        return "增值税已认证进项税".equals(normalizedBaseItem);
    }

    private boolean isOpeningRetainedInputTax(String normalizedBaseItem) {
        return "期初留抵进项税".equals(normalizedBaseItem);
    }

    private boolean isEndingRetainedInputTax(String normalizedBaseItem) {
        return "期末留抵进项税".equals(normalizedBaseItem);
    }

    private boolean isVatPayable(String normalizedBaseItem) {
        return "应交增值税".equals(normalizedBaseItem);
    }

    private boolean isCurrentPeriodVatPayable(String normalizedBaseItem) {
        return "本期应交增值税".equals(normalizedBaseItem);
    }

    private boolean isPrepaidDeduction(String normalizedBaseItem) {
        return "异地预缴递减".equals(normalizedBaseItem);
    }

    private boolean isInputTransferOut(String normalizedBaseItem) {
        return "进项转出".equals(normalizedBaseItem);
    }

    private String normalizeBaseItem(String baseItem) {
        String value = trim(baseItem);
        if (value == null) {
            return "";
        }
        return value.replace("*", "").replace("＊", "").replace(" ", "");
    }

    private boolean isTotalRow(String splitBasis) {
        return containsIgnoreCase(splitBasis, "合计");
    }

    private String extractRateText(String splitBasis) {
        if (isBlank(splitBasis)) {
            return null;
        }
        Matcher matcher = RATE_PATTERN.matcher(splitBasis);
        if (!matcher.find()) {
            return null;
        }
        return matcher.group(1) + "%";
    }

    private BigDecimal rateTextToDecimal(String rateText) {
        String text = trim(rateText);
        if (isBlank(text)) {
            return null;
        }
        String normalized = text.replace("％", "%");
        if (normalized.endsWith("%")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        BigDecimal value = new BigDecimal(normalized);
        return value.divide(BigDecimal.valueOf(100), 8, java.math.RoundingMode.HALF_UP);
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
