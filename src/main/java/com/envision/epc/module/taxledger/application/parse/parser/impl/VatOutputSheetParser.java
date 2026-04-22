package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.envision.epc.module.taxledger.application.dto.VatOutputSheetUploadDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增值税销项解析器（双section）。
 * 对应sheet页：增值税销项
 * 对应类别：FileCategoryEnum.VAT_OUTPUT
 */
@Component
public class VatOutputSheetParser implements SheetParser<VatOutputSheetUploadDTO> {
    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_OUTPUT;
    }

    @Override
    public Class<VatOutputSheetUploadDTO> resultType() {
        return VatOutputSheetUploadDTO.class;
    }

    @Override
    public ParseResult<VatOutputSheetUploadDTO> parse(InputStream inputStream, ParseContext context) {
        VatOutputSheetUploadDTO data = new VatOutputSheetUploadDTO();
        data.setInvoiceDetails(new ArrayList<>());
        data.setTaxRateSummaries(new ArrayList<>());

        ParseResult<VatOutputSheetUploadDTO> result = ParseResult.<VatOutputSheetUploadDTO>builder().data(data).build();
        if (inputStream == null) {
            result.addIssue("增值税销项：文件流为空");
            return result;
        }

        List<Map<Integer, String>> rows = new ArrayList<>();
        try {
            EasyExcelFactory.read(inputStream, new AnalysisEventListener<Map<Integer, String>>() {
                        @Override
                        public void invoke(Map<Integer, String> rowData, AnalysisContext analysisContext) {
                            rows.add(new HashMap<>(rowData));
                        }

                        @Override
                        public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                            // no-op
                        }
                    })
                    .headRowNumber(0)
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            result.addIssue("增值税销项：读取Excel失败 - " + e.getMessage());
            return result;
        }

        if (rows.isEmpty()) {
            result.addIssue("增值税销项：无可解析数据");
            return result;
        }

        int section1HeaderIdx = findHeaderRow(rows, List.of("序号", "数电发票号码", "税收分类编码"));
        int section2HeaderIdx = findHeaderRow(rows, List.of("税率/征收率", "开具蓝字发票金额", "作废红字发票税额"));
        int section2TitleIdx = findHeaderRow(rows, List.of("按税率（征收率）统计表"));
        if (section1HeaderIdx < 0) {
            result.addIssue("增值税销项：未识别到发票明细表头");
            return result;
        }
        if (section2HeaderIdx < 0) {
            result.addIssue("增值税销项：未识别到按税率统计表头");
            return result;
        }
        if (section2HeaderIdx <= section1HeaderIdx) {
            result.addIssue("增值税销项：表结构异常，统计表头位置错误");
            return result;
        }
        if (section2TitleIdx < 0 || section2TitleIdx > section2HeaderIdx) {
            section2TitleIdx = section2HeaderIdx;
        }

        Map<String, Integer> section1Cols = buildHeaderIndex(rows.get(section1HeaderIdx));
        Map<String, Integer> section2Cols = buildHeaderIndex(rows.get(section2HeaderIdx));

        parseSection1(rows, section1HeaderIdx + 1, section2TitleIdx - 1, section1Cols, data);
        parseSection2(rows, section2HeaderIdx + 1, rows.size() - 1, section2Cols, data);
        return result;
    }

    private static void parseSection1(List<Map<Integer, String>> rows,
                                      int startIdx,
                                      int endIdx,
                                      Map<String, Integer> cols,
                                      VatOutputSheetUploadDTO target) {
        Integer serialNoCol = cols.get("序号");
        Integer digitalInvoiceNoCol = cols.get("数电发票号码");
        Integer sellerTaxpayerIdCol = cols.get("销方识别号");
        Integer sellerNameCol = cols.get("销方名称");
        Integer buyerTaxpayerIdCol = cols.get("购方识别号");
        Integer buyerNameCol = firstPresent(cols, "购买方名称", "购方名称");
        Integer invoiceDateCol = cols.get("开票日期");
        Integer taxClassificationCodeCol = cols.get("税收分类编码");
        Integer specificBusinessTypeCol = cols.get("特定业务类型");
        Integer invoiceCodeCol = cols.get("发票代码");
        Integer invoiceNoCol = cols.get("发票号码");

        for (int i = startIdx; i <= endIdx && i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }

            VatOutputSheetUploadDTO.InvoiceDetailItem item = new VatOutputSheetUploadDTO.InvoiceDetailItem();
            item.setSerialNo(get(row, serialNoCol));
            item.setInvoiceCode(get(row, invoiceCodeCol));
            item.setInvoiceNo(get(row, invoiceNoCol));
            item.setDigitalInvoiceNo(get(row, digitalInvoiceNoCol));
            item.setSellerTaxpayerId(get(row, sellerTaxpayerIdCol));
            item.setSellerName(get(row, sellerNameCol));
            item.setBuyerTaxpayerId(get(row, buyerTaxpayerIdCol));
            item.setBuyerName(get(row, buyerNameCol));
            item.setInvoiceDate(get(row, invoiceDateCol));
            item.setTaxClassificationCode(get(row, taxClassificationCodeCol));
            item.setSpecificBusinessType(get(row, specificBusinessTypeCol));
            target.getInvoiceDetails().add(item);
        }
    }

    private static void parseSection2(List<Map<Integer, String>> rows,
                                      int startIdx,
                                      int endIdx,
                                      Map<String, Integer> cols,
                                      VatOutputSheetUploadDTO target) {
        Integer serialNoCol = cols.get("序号");
        Integer invoiceStatusCol = firstPresent(cols, "发票种类", "发票状态");
        Integer taxRateCol = cols.get("税率/征收率");
        Integer blueAmountCol = cols.get("开具蓝字发票金额");
        Integer blueTaxAmountCol = cols.get("开具蓝字发票税额");
        Integer canceledBlueAmountCol = cols.get("作废蓝字发票金额");
        Integer canceledBlueTaxAmountCol = cols.get("作废蓝字发票税额");
        Integer redAmountCol = cols.get("开具红字发票金额");
        Integer redTaxAmountCol = cols.get("开具红字发票税额");
        Integer canceledRedAmountCol = cols.get("作废红字发票金额");
        Integer canceledRedTaxAmountCol = cols.get("作废红字发票税额");

        for (int i = startIdx; i <= endIdx && i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }

            VatOutputSheetUploadDTO.TaxRateSummaryItem item = new VatOutputSheetUploadDTO.TaxRateSummaryItem();
            item.setSerialNo(get(row, serialNoCol));
            item.setInvoiceStatus(get(row, invoiceStatusCol));
            item.setTaxRateOrLevyRate(parseRate(get(row, taxRateCol)));
            item.setBlueInvoiceAmount(ParserValueUtils.toBigDecimal(get(row, blueAmountCol)));
            item.setBlueInvoiceTaxAmount(ParserValueUtils.toBigDecimal(get(row, blueTaxAmountCol)));
            item.setCanceledBlueInvoiceAmount(ParserValueUtils.toBigDecimal(get(row, canceledBlueAmountCol)));
            item.setCanceledBlueInvoiceTaxAmount(ParserValueUtils.toBigDecimal(get(row, canceledBlueTaxAmountCol)));
            item.setRedInvoiceAmount(ParserValueUtils.toBigDecimal(get(row, redAmountCol)));
            item.setRedInvoiceTaxAmount(ParserValueUtils.toBigDecimal(get(row, redTaxAmountCol)));
            item.setCanceledRedInvoiceAmount(ParserValueUtils.toBigDecimal(get(row, canceledRedAmountCol)));
            item.setCanceledRedInvoiceTaxAmount(ParserValueUtils.toBigDecimal(get(row, canceledRedTaxAmountCol)));
            target.getTaxRateSummaries().add(item);
        }
    }

    private static int findHeaderRow(List<Map<Integer, String>> rows, List<String> mustContains) {
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            if (row == null || row.isEmpty()) {
                continue;
            }
            boolean matched = true;
            for (String key : mustContains) {
                if (!containsCellValue(row, key)) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return i;
            }
        }
        return -1;
    }

    private static Map<String, Integer> buildHeaderIndex(Map<Integer, String> row) {
        Map<String, Integer> map = new HashMap<>();
        if (row == null) {
            return map;
        }
        for (Map.Entry<Integer, String> entry : row.entrySet()) {
            String v = normalize(entry.getValue());
            if (StringUtils.hasText(v)) {
                map.putIfAbsent(v, entry.getKey());
            }
        }
        return map;
    }

    private static Integer firstPresent(Map<String, Integer> cols, String... names) {
        for (String name : names) {
            Integer idx = cols.get(name);
            if (idx != null) {
                return idx;
            }
        }
        return null;
    }

    private static boolean containsCellValue(Map<Integer, String> row, String expected) {
        String normalizedExpected = normalize(expected);
        for (String value : row.values()) {
            if (normalizedExpected.equals(normalize(value))) {
                return true;
            }
        }
        return false;
    }

    private static String get(Map<Integer, String> row, Integer col) {
        if (row == null || col == null) {
            return null;
        }
        return normalize(row.get(col));
    }

    private static boolean isBlankRow(Map<Integer, String> row) {
        if (row == null || row.isEmpty()) {
            return true;
        }
        for (String value : row.values()) {
            if (StringUtils.hasText(normalize(value))) {
                return false;
            }
        }
        return true;
    }

    private static BigDecimal parseRate(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String normalized = raw.trim().replace("％", "%").replace(",", "");
        boolean percent = normalized.endsWith("%");
        if (percent) {
            normalized = normalized.substring(0, normalized.length() - 1).trim();
        }
        BigDecimal val = ParserValueUtils.toBigDecimal(normalized);
        if (val == null) {
            return null;
        }
        if (percent) {
            return val.divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
        }
        return val;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\u00A0", " ").trim();
    }
}
