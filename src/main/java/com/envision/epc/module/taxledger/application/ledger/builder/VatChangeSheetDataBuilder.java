package com.envision.epc.module.taxledger.application.ledger.builder;

import com.envision.epc.facade.azure.BlobStorageRemote;
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
import com.envision.epc.module.taxledger.application.ledger.data.PlAppendix2320LedgerSheetData;
import com.envision.epc.module.taxledger.application.ledger.data.VatChangeLedgerSheetData;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.SheetParseService;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import com.envision.epc.module.taxledger.domain.FileRecord;
import com.envision.epc.module.taxledger.domain.VatBasicItemConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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
 * 增值税变动表页数据构建器。
 * <p>对应台账 Sheet：{@link LedgerSheetCode#VAT_CHANGE}；负责“基础条目+拆分依据”结构行生成与金额口径计算。</p>
 */
@Component
@RequiredArgsConstructor
public class VatChangeSheetDataBuilder implements LedgerSheetDataBuilder<VatChangeLedgerSheetData> {
    /** 拆分依据中的税率提取规则，例如“13%”“9％”。 */
    private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]");
    /** 数据湖进项明细中“进项转出”对应的总账科目。 */
    private static final String ACCOUNT_INPUT_TRANSFER_OUT = "2221010400";
    /** 增值税销项统计中“专票”识别关键字。 */
    private static final String INVOICE_TYPE_SPECIAL = "增值税专用发票";
    /** 增值税销项统计中“普票”识别关键字。 */
    private static final String INVOICE_TYPE_GENERAL = "普通发票";
    private final PreviousLedgerLocator previousLedgerLocator;
    private final BlobStorageRemote blobStorageRemote;
    private final SheetParseService sheetParseService;

    /** 当前 Builder 对应的台账页编码。 */
    @Override
    public LedgerSheetCode support() {
        return LedgerSheetCode.VAT_CHANGE;
    }

    /**
     * 组装【增值税变动表】页最终渲染数据。
     * <p>执行顺序：取源数据 -> 构建结构行 -> 计算合计 -> 计算当月开票/以前月度开票/未开票 -> 金额归一化。</p>
     */
    @Override
    public VatChangeLedgerSheetData build(LedgerBuildContext ctx) {
        // 公司分支：2320/2355 与项目公司计算口径不同。
        boolean is2320Or2355 = isCompany2320Or2355(ctx.getCompanyCode());

        // ===== 读取本页计算所需的基础输入（来自 context 预加载） =====
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

        // ===== 按公司类型读取差异化输入 =====
        PlAppendix23202355DTO plAppendix2320 = null;
        MonthlySettlementTaxParsedDTO monthlySettlementTax = null;
        List<PlAppendixProjectCompanyUploadDTO> plAppendixProject = List.of();
        if (is2320Or2355) {
            // 2320/2355：必须消费“PL附表2320 Builder 产物”，不能直接消费原始解析镜像。
            PlAppendix2320LedgerSheetData plAppendix2320Data = ctx.requireBuilt(
                    LedgerSheetCode.PL_APPENDIX_2320, PlAppendix2320LedgerSheetData.class, LedgerSheetCode.VAT_CHANGE);
            plAppendix2320 = plAppendix2320Data == null ? null : plAppendix2320Data.getPayload();
            // 同时需要月结报税聚合结果做专票口径校验。
            monthlySettlementTax = SheetDataReaders.requireObject(
                    ctx, FileCategoryEnum.MONTHLY_SETTLEMENT_TAX, MonthlySettlementTaxParsedDTO.class, LedgerSheetCode.VAT_CHANGE);
        } else {
            // 项目公司：读取项目公司 PL 附表解析数据。
            plAppendixProject = SheetDataReaders.requireList(
                    ctx, FileCategoryEnum.PL_APPENDIX_PROJECT, PlAppendixProjectCompanyUploadDTO.class, LedgerSheetCode.VAT_CHANGE);
        }

        // 1) 先按配置构建“基础条目 + 拆分依据”结构行。
        List<VatChangeRowDTO> rows = buildStructureRows(ctx, is2320Or2355, plAppendix2320, plAppendixProject);
        // 2) 回填“合计”列（总口径输入）。
        fillTotalAmount(ctx, rows, is2320Or2355, plAppendix2320, plAppendixProject, plRows, bsRows, vatInputCert, vatChangeAppendix, dlInput);
        // 3) 回填“当月开票金额”列。
        fillCurrentMonthInvoicedAmount(rows, is2320Or2355, vatOutput, monthlySettlementTax);
        // 4) 回填“以前月度开票金额”（当前实现统一为 0）。
        fillPreviousMonthInvoicedAmount(rows);
        // 5) 计算“未开票金额”。
        fillUnbilledAmount(rows);
        // 6) 金额字段统一零值化，避免 renderer 层出现 null 分支。
        normalizeAmountFields(rows);

        // 渲染时下方还要贴“增值税变动表附表”，这里记录其最新源文件 blobPath。
        String appendixBlobPath = findLatestBlobPath(ctx, FileCategoryEnum.VAT_CHANGE_APPENDIX);
        return new VatChangeLedgerSheetData(appendixBlobPath, null, rows);
    }

    /**
     * 按“基础条目配置表”生成主表结构行。
     * <p>仅决定有哪些行，不做金额计算。</p>
     */
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
                .filter(cfg -> matchesCompanyCode(cfg.getCompanyCode(), ctx.getCompanyCode()))
                .sorted(Comparator.comparing(cfg -> cfg.getItemSeq() == null ? Integer.MAX_VALUE : cfg.getItemSeq()))
                .toList();

        // 拆分依据来源按公司类型区分：2320/2355 来自 PL附表2320，项目公司来自项目公司附表。
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

    /**
     * 计算并回填“合计”列。
     * <p>分三段执行：先填原子口径，再计算派生口径（期末留抵），最后计算“应交增值税/本期应交增值税”。</p>
     */
    private void fillTotalAmount(LedgerBuildContext ctx,
                                 List<VatChangeRowDTO> rows,
                                 boolean is2320Or2355,
                                 PlAppendix23202355DTO plAppendix2320,
                                 List<PlAppendixProjectCompanyUploadDTO> plAppendixProject,
                                 List<PlStatementRowDTO> plRows,
                                 List<BsStatementRowDTO> bsRows,
                                 VatInputCertParsedDTO vatInputCert,
                                 VatChangeAppendixUploadDTO vatChangeAppendix,
                                 DlInputParsedDTO dlInput) {
        // ===== 第一段：逐行填充原子来源值 =====
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
                row.setTotalAmount(nvl(vatInputCert == null ? null : vatInputCert.getAmountSum()));
                continue;
            }
            if (isOpeningRetainedInputTax(base)) {
                row.setTotalAmount(resolveOpeningRetainedInputTax(ctx));
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

        // ===== 第二段：计算“期末留抵进项税” =====
        // 公式：X = 应交增值税-销项税 - (增值税已认证进项税 + 期初留抵进项税 - 进项转出)
        // 若 X < 0，则期末留抵 = |X|；否则为 0。
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
        // ===== 第三段：计算“应交增值税”与“本期应交增值税” =====
        // 应交增值税规则：期末留抵>0 时强制为 0，否则按口径汇总计算。
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
                        .subtract(sumByBaseItem(rows, "异地预缴抵减")));
            }
        }
    }

    /**
     * 计算并回填“当月开票金额”列。
     * <p>2320/2355：按拆分依据税率 + 专票/普票分流，并对专票执行“销项表 vs 月结报税”校验；
     * 项目公司：直接按销项表合计口径回填。</p>
     */
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
                // 2320/2355：从拆分依据提取税率，作为销项表明细命中条件。
                String splitBasis = trim(row.getSplitBasis());
                String rateText = extractRateText(splitBasis);
                if (isBlank(rateText)) {
                    throw new BizException(ErrorCode.BAD_REQUEST, "增值税变动表：拆分依据无法提取税率，baseItem="
                            + row.getBaseItem() + ", splitBasis=" + splitBasis);
                }
                BigDecimal rate = rateTextToDecimal(rateText);
                boolean general = containsIgnoreCase(splitBasis, "普票");
                if (isMainBusinessRevenue(base)) {
                    // 主营业务收入：金额口径（blueInvoiceAmount）。
                    if (general) {
                        row.setCurrentMonthInvoicedAmount(
                                findVatOutputValue(vatOutput, rate, INVOICE_TYPE_GENERAL, true));
                    } else {
                        // 专票：要求销项表与月结报税同口径一致。
                        BigDecimal vatValue = findVatOutputValue(vatOutput, rate, INVOICE_TYPE_SPECIAL, true);
                        BigDecimal monthlyValue = sumMonthlyByRate(monthlySettlementTax, rateText, true);
                        ensureAmountEquals(vatValue, monthlyValue,
                                "当月利润表主营业务收入*的当月开票金额【增值税销项表-按税率（征收率）统计表】与【睿景景程月结数据表-报税】不一致，请检查。");
                        row.setCurrentMonthInvoicedAmount(vatValue);
                    }
                } else {
                    // 应交增值税-销项税：税额口径（blueInvoiceTaxAmount）。
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
                // 项目公司：直接使用销项表“合计行”，若无合计行则全表求和。
                if (isMainBusinessRevenue(base)) {
                    row.setCurrentMonthInvoicedAmount(sumVatOutputTotal(vatOutput, true));
                } else if (isOutputTaxPayable(base)) {
                    row.setCurrentMonthInvoicedAmount(sumVatOutputTotal(vatOutput, false));
                }
            }
        }
    }

    /** 回填“以前月度开票金额”，当前版本统一置 0。 */
    private void fillPreviousMonthInvoicedAmount(List<VatChangeRowDTO> rows) {
        for (VatChangeRowDTO row : rows) {
            row.setPreviousMonthInvoicedAmount(BigDecimal.ZERO);
        }
    }

    /** 计算“未开票金额”= 合计 - 当月开票 - 以前月度开票；非目标条目固定为 0。 */
    private void fillUnbilledAmount(List<VatChangeRowDTO> rows) {
        for (VatChangeRowDTO row : rows) {
            String base = normalizeBaseItem(row.getBaseItem());
            if (!isMainBusinessRevenue(base) && !isOutputTaxPayable(base)) {
                row.setUnbilledAmount(BigDecimal.ZERO);
                continue;
            }
            BigDecimal unbilled = nvl(row.getTotalAmount())
                    .subtract(nvl(row.getCurrentMonthInvoicedAmount()))
                    .subtract(nvl(row.getPreviousMonthInvoicedAmount()));
            row.setUnbilledAmount(unbilled);
        }
    }

    /** 金额字段统一做 NVL，确保后续渲染层看到的都是数值类型。 */
    private void normalizeAmountFields(List<VatChangeRowDTO> rows) {
        for (VatChangeRowDTO row : rows) {
            row.setTotalAmount(nvl(row.getTotalAmount()));
            row.setCurrentMonthInvoicedAmount(nvl(row.getCurrentMonthInvoicedAmount()));
            row.setPreviousMonthInvoicedAmount(nvl(row.getPreviousMonthInvoicedAmount()));
            row.setUnbilledAmount(nvl(row.getUnbilledAmount()));
        }
    }

    /** 创建一行增值税变动表行对象，并生成默认 itemName。 */
    private VatChangeRowDTO newRow(String baseItem, String splitBasis) {
        VatChangeRowDTO row = new VatChangeRowDTO();
        row.setBaseItem(baseItem);
        row.setSplitBasis(splitBasis);
        row.setItemName(isBlank(splitBasis) ? baseItem : baseItem + "-" + splitBasis);
        return row;
    }

    /** 从 2320/2355 PL附表中提取拆分依据，去重并保序，过滤“合计”行。 */
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

    /** 从项目公司 PL附表中提取拆分依据，去重并保序。 */
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

    /** 依据拆分依据匹配 2320/2355 附表“申报金额”。 */
    private BigDecimal findDeclaredAmountBySplitBasis(PlAppendix23202355DTO dto, String splitBasis) {
        if (dto == null || dto.getDeclarationSplitList() == null) {
            return null;
        }
        PlAppendix23202355DTO.DeclarationSplitItem matched = findDeclarationRow(dto.getDeclarationSplitList(), splitBasis);
        return matched == null ? null : matched.getDeclaredAmount();
    }

    /** 依据拆分依据匹配 2320/2355 附表“申报税额”。 */
    private BigDecimal findDeclaredTaxAmountBySplitBasis(PlAppendix23202355DTO dto, String splitBasis) {
        if (dto == null || dto.getDeclarationSplitList() == null) {
            return null;
        }
        PlAppendix23202355DTO.DeclarationSplitItem matched = findDeclarationRow(dto.getDeclarationSplitList(), splitBasis);
        return matched == null ? null : matched.getDeclaredTaxAmount();
    }

    /** 按“先精确后模糊包含”规则匹配 2320/2355 申报拆分行。 */
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

    /** 项目公司：按拆分依据匹配主营业务收入。 */
    private BigDecimal findProjectAppendixMainBusinessRevenue(List<PlAppendixProjectCompanyUploadDTO> rows, String splitBasis) {
        PlAppendixProjectCompanyUploadDTO row = findProjectAppendixRow(rows, splitBasis);
        return row == null ? null : row.getMainBusinessRevenue();
    }

    /** 项目公司：按拆分依据匹配销项税额。 */
    private BigDecimal findProjectAppendixOutputTax(List<PlAppendixProjectCompanyUploadDTO> rows, String splitBasis) {
        PlAppendixProjectCompanyUploadDTO row = findProjectAppendixRow(rows, splitBasis);
        return row == null ? null : row.getOutputTax();
    }

    /** 项目公司附表行匹配，规则同 2320：先精确再模糊。 */
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

    /** PL 回退口径：汇总“主营业务收入”本期发生额。 */
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

    /** BS 回退口径：应交税费*(累计发生数-年初数)*-1。 */
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

    /** 从销项统计表中按“税率+发票类型”提取金额或税额。 */
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

    /** 从月结报税聚合数据中按税率汇总开票收入/开票税额，用于专票一致性校验。 */
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

    /** 项目公司口径：优先销项表“合计行”，找不到则整表累加。 */
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

    /** 口径一致性校验，不一致直接抛业务异常阻断。 */
    private void ensureAmountEquals(BigDecimal left, BigDecimal right, String message) {
        if (nvl(left).compareTo(nvl(right)) != 0) {
            throw new BizException(ErrorCode.BAD_REQUEST, message);
        }
    }

    /** 按基础条目归一化后汇总“合计”金额。 */
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

    /**
     * 解析“期初留抵进项税”：
     * <p>从最近前序最终台账读取【增值税变动表】并汇总“期末留抵进项税”行；若无前序或读取失败则按 0 处理。</p>
     */
    private BigDecimal resolveOpeningRetainedInputTax(LedgerBuildContext ctx) {
        String normalizedYm = PreviousLedgerLocator.normalizeYearMonth(ctx.getYearMonth());
        PreviousLedgerLocator.PreviousLedgerRef previous = previousLedgerLocator.find(ctx.getCompanyCode(), normalizedYm);
        if (previous == null || isBlank(previous.ledgerBlobPath())) {
            return BigDecimal.ZERO;
        }
        List<VatChangeRowDTO> previousRows = parsePreviousVatChangeRows(ctx, previous.ledgerBlobPath());
        if (previousRows.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (VatChangeRowDTO row : previousRows) {
            if (row == null) {
                continue;
            }
            if (isEndingRetainedInputTax(normalizeBaseItem(row.getBaseItem()))) {
                // 前序台账“期末留抵进项税”即本期“期初留抵进项税”来源。
                sum = sum.add(nvl(row.getTotalAmount()));
            }
        }
        return sum;
    }

    /** 下载并解析前序台账中的【增值税变动表】行数据；异常时返回空列表（不中断）。 */
    private List<VatChangeRowDTO> parsePreviousVatChangeRows(LedgerBuildContext ctx, String blobPath) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            blobStorageRemote.loadStream(blobPath, out);
            ParseResult<?> parsed = sheetParseService.parse(
                    new ByteArrayInputStream(out.toByteArray()),
                    FileCategoryEnum.VAT_CHANGE,
                    ParseContext.builder()
                            .companyCode(ctx.getCompanyCode())
                            .yearMonth(ctx.getYearMonth())
                            .fileName("previous-final-ledger")
                            .operator(ctx.getOperator())
                            .traceId(ctx.getTraceId())
                            .build()
            );
            if (parsed == null || parsed.hasError() || !(parsed.getData() instanceof List<?> list)) {
                return List.of();
            }
            List<VatChangeRowDTO> rows = new ArrayList<>();
            for (Object item : list) {
                if (item instanceof VatChangeRowDTO dto) {
                    rows.add(dto);
                }
            }
            return rows;
        } catch (Exception ex) {
            return List.of();
        }
    }

    /** 读取当前任务中“增值税变动表附表”最新源文件 blobPath。 */
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

    /** 是否为 2320/2355 公司。 */
    private boolean isCompany2320Or2355(String companyCode) {
        return "2320".equals(companyCode) || "2355".equals(companyCode);
    }

    /** 基础条目是否为“当月利润表主营业务收入”。 */
    private boolean isMainBusinessRevenue(String normalizedBaseItem) {
        return "当月利润表主营业务收入".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“应交增值税-销项税”。 */
    private boolean isOutputTaxPayable(String normalizedBaseItem) {
        return "应交增值税-销项税".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“增值税已认证进项税”。 */
    private boolean isCertifiedInputTax(String normalizedBaseItem) {
        return "增值税已认证进项税".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“期初留抵进项税”。 */
    private boolean isOpeningRetainedInputTax(String normalizedBaseItem) {
        return "期初留抵进项税".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“期末留抵进项税”。 */
    private boolean isEndingRetainedInputTax(String normalizedBaseItem) {
        return "期末留抵进项税".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“应交增值税”。 */
    private boolean isVatPayable(String normalizedBaseItem) {
        return "应交增值税".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“本期应交增值税”。 */
    private boolean isCurrentPeriodVatPayable(String normalizedBaseItem) {
        return "本期应交增值税".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“异地预缴抵减”。 */
    private boolean isPrepaidDeduction(String normalizedBaseItem) {
        return "异地预缴抵减".equals(normalizedBaseItem);
    }

    /** 基础条目是否为“进项转出”。 */
    private boolean isInputTransferOut(String normalizedBaseItem) {
        return "进项转出".equals(normalizedBaseItem);
    }

    /** 归一化基础条目：去空格、去星号，便于规则匹配。 */
    private String normalizeBaseItem(String baseItem) {
        String value = trim(baseItem);
        if (value == null) {
            return "";
        }
        return value.replace("*", "").replace("＊", "").replace(" ", "");
    }

    /** 是否“合计”行。 */
    private boolean isTotalRow(String splitBasis) {
        return containsIgnoreCase(splitBasis, "合计");
    }

    /** 从拆分依据文本中提取税率文本（例如 13%）。 */
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

    /** 将税率文本（如 13%）转换为小数（0.13）。 */
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

    /** 忽略大小写包含判断。 */
    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) {
            return false;
        }
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }

    /** BigDecimal 空值转 0。 */
    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 安全 trim。 */
    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    /** 判空白字符串。 */
    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /** 配置公司编码匹配（支持英文/中文逗号分隔）。 */
    private boolean matchesCompanyCode(String configCompanyCode, String companyCode) {
        String target = trim(companyCode);
        if (isBlank(target)) {
            return false;
        }
        String cfg = trim(configCompanyCode);
        if (isBlank(cfg)) {
            return true;
        }
        String normalized = cfg.replace('，', ',');
        String[] parts = normalized.split(",");
        for (String part : parts) {
            if (target.equals(trim(part))) {
                return true;
            }
        }
        return false;
    }
}
