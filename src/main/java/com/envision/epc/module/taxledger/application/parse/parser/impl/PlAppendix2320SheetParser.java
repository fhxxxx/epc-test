package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.alibaba.excel.EasyExcelFactory;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.envision.epc.module.taxledger.application.dto.PlAppendix23202355DTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * PL附表-2320、2355 解析器。
 */
@Component
public class PlAppendix2320SheetParser implements SheetParser<PlAppendix23202355DTO> {
    private static final Pattern RATE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)\\s*%");


    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.PL_APPENDIX_2320;
    }

    @Override
    public Class<PlAppendix23202355DTO> resultType() {
        return PlAppendix23202355DTO.class;
    }

    @Override
    public ParseResult<PlAppendix23202355DTO> parse(InputStream inputStream, ParseContext context) {
        PlAppendix23202355DTO data = new PlAppendix23202355DTO();
        data.setInvoicingSplitList(new ArrayList<>());
        data.setDeclarationSplitList(new ArrayList<>());
        ParseResult<PlAppendix23202355DTO> result = ParseResult.<PlAppendix23202355DTO>builder().data(data).build();
        if (inputStream == null) {
            result.addIssue("PL附表-2320、2355：文件流为空");
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
                        }
                    })
                    .headRowNumber(0)
                    .sheet(category().getTargetSheetName())
                    .doRead();
        } catch (Exception e) {
            result.addIssue("PL附表-2320、2355：读取Excel失败 - " + e.getMessage());
            return result;
        }
        if (rows.isEmpty()) {
            result.addIssue("PL附表-2320、2355：无可解析数据");
            return result;
        }

        int section1Header = findHeaderRow(rows, List.of("拆分依据", "未开票收入", "销项", "已开票收入", "已开票销项"));
        int section2Header = findHeaderRow(rows, List.of("拆分依据", "申报金额", "申报税额"));
        if (section1Header < 0) {
            result.addIssue("PL附表-2320、2355：未识别到Section1表头");
            return result;
        }
        if (section2Header < 0) {
            result.addIssue("PL附表-2320、2355：未识别到Section2表头");
            return result;
        }

        Map<String, Integer> section1Cols = buildHeaderIndex(rows.get(section1Header));
        Map<String, Integer> section2Cols = buildHeaderIndex(rows.get(section2Header));

        int section1End = section2Header - 1;
        Set<String> section1BasisKeys = new HashSet<>();
        Set<String> section2BasisKeys = new HashSet<>();

        parseSection1(rows, section1Header + 1, section1End, section1Cols, data, result, section1BasisKeys);
        parseSection2(rows, section2Header + 1, rows.size() - 1, section2Cols, data, result, section2BasisKeys);
        validateSectionBasisConsistency(section1BasisKeys, section2BasisKeys, result);
        return result;
    }

    private static void parseSection1(List<Map<Integer, String>> rows,
                                      int startRow,
                                      int endRow,
                                      Map<String, Integer> cols,
                                      PlAppendix23202355DTO target,
                                      ParseResult<PlAppendix23202355DTO> result,
                                      Set<String> sectionBasisKeys) {
        Integer splitBasisCol = cols.get("拆分依据");
        Integer uninvoicedIncomeCol = cols.get("未开票收入");
        Integer outputTaxCol = cols.get("销项");
        Integer invoicedIncomeCol = cols.get("已开票收入");
        Integer invoicedOutputTaxCol = cols.get("已开票销项");

        for (int i = startRow; i <= endRow && i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            String splitBasis = get(row, splitBasisCol);
            if (isTotalRow(splitBasis)) {
                continue;
            }
            if (isBlankValue(splitBasis)) {
                continue;
            }
            validateBasisWithRate(splitBasis, result, i + 1, "Section1");

            // 普票行允许部分字段空，但若后续字段整行全空则自动忽略该拆分依据
            if (containsPuPiao(splitBasis)
                    && isAllBlank(get(row, uninvoicedIncomeCol), get(row, outputTaxCol), get(row, invoicedIncomeCol), get(row, invoicedOutputTaxCol))) {
                continue;
            }
            validateDuplicateBasis(splitBasis, sectionBasisKeys, result, i + 1, "Section1");

            PlAppendix23202355DTO.InvoicingSplitItem item = new PlAppendix23202355DTO.InvoicingSplitItem();
            item.setSplitBasis(splitBasis);
            item.setUninvoicedIncome(ParserValueUtils.toBigDecimal(get(row, uninvoicedIncomeCol)));
            item.setOutputTax(ParserValueUtils.toBigDecimal(get(row, outputTaxCol)));
            item.setInvoicedIncome(ParserValueUtils.toBigDecimal(get(row, invoicedIncomeCol)));
            item.setInvoicedOutputTax(ParserValueUtils.toBigDecimal(get(row, invoicedOutputTaxCol)));
            target.getInvoicingSplitList().add(item);
        }
    }

    private static void parseSection2(List<Map<Integer, String>> rows,
                                      int startRow,
                                      int endRow,
                                      Map<String, Integer> cols,
                                      PlAppendix23202355DTO target,
                                      ParseResult<PlAppendix23202355DTO> result,
                                      Set<String> sectionBasisKeys) {
        Integer splitBasisCol = cols.get("拆分依据");
        Integer declaredAmountCol = cols.get("申报金额");
        Integer declaredTaxAmountCol = cols.get("申报税额");

        for (int i = startRow; i <= endRow && i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            if (isBlankRow(row)) {
                continue;
            }
            String splitBasis = get(row, splitBasisCol);
            if (isTotalRow(splitBasis)) {
                continue;
            }
            if (isBlankValue(splitBasis)) {
                continue;
            }
            validateBasisWithRate(splitBasis, result, i + 1, "Section2");

            // 普票行允许部分字段空，但若后续字段整行全空则自动忽略该拆分依据
            if (containsPuPiao(splitBasis)
                    && isAllBlank(get(row, declaredAmountCol), get(row, declaredTaxAmountCol))) {
                continue;
            }
            validateDuplicateBasis(splitBasis, sectionBasisKeys, result, i + 1, "Section2");

            PlAppendix23202355DTO.DeclarationSplitItem item = new PlAppendix23202355DTO.DeclarationSplitItem();
            item.setSplitBasis(splitBasis);
            item.setDeclaredAmount(ParserValueUtils.toBigDecimal(get(row, declaredAmountCol)));
            item.setDeclaredTaxAmount(ParserValueUtils.toBigDecimal(get(row, declaredTaxAmountCol)));
            target.getDeclarationSplitList().add(item);
        }
    }

    private static void validateSectionBasisConsistency(Set<String> section1BasisKeys,
                                                        Set<String> section2BasisKeys,
                                                        ParseResult<PlAppendix23202355DTO> result) {
        Set<String> missingInSection2 = new HashSet<>(section1BasisKeys);
        missingInSection2.removeAll(section2BasisKeys);
        if (!missingInSection2.isEmpty()) {
            result.addIssue("PL附表-2320、2355：Section1 与 Section2 拆分依据不一致，Section2缺少: " + String.join(", ", missingInSection2));
        }

        Set<String> missingInSection1 = new HashSet<>(section2BasisKeys);
        missingInSection1.removeAll(section1BasisKeys);
        if (!missingInSection1.isEmpty()) {
            result.addIssue("PL附表-2320、2355：Section1 与 Section2 拆分依据不一致，Section1缺少: " + String.join(", ", missingInSection1));
        }
    }

    private static void validateBasisWithRate(String splitBasis,
                                              ParseResult<PlAppendix23202355DTO> result,
                                              int rowNo,
                                              String section) {
        if (!RATE_PATTERN.matcher(splitBasis).find()) {
            result.addIssue("PL附表-2320、2355：" + section + " 第" + rowNo + "行拆分依据未包含税率(%): " + splitBasis);
        }
    }

    private static void validateDuplicateBasis(String splitBasis,
                                               Set<String> sectionBasisKeys,
                                               ParseResult<PlAppendix23202355DTO> result,
                                               int rowNo,
                                               String section) {
        String key = normalizeBasisKey(splitBasis);
        if (!sectionBasisKeys.add(key)) {
            result.addIssue("PL附表-2320、2355：" + section + " 第" + rowNo + "行拆分依据重复: " + splitBasis);
        }
    }

    private static String normalizeBasisKey(String splitBasis) {
        if (splitBasis == null) {
            return "";
        }
        return splitBasis.replace(" ", "").replace("　", "").trim().toLowerCase();
    }

    private static boolean containsPuPiao(String splitBasis) {
        return splitBasis != null && splitBasis.contains("普票");
    }

    private static boolean isTotalRow(String splitBasis) {
        return splitBasis != null && splitBasis.contains("合计");
    }

    private static boolean isAllBlank(String... values) {
        for (String value : values) {
            if (!isBlankValue(value)) {
                return false;
            }
        }
        return true;
    }

    private static boolean isBlankValue(String value) {
        return !StringUtils.hasText(normalize(value));
    }

    private static int findHeaderRow(List<Map<Integer, String>> rows, List<String> requiredHeaders) {
        for (int i = 0; i < rows.size(); i++) {
            Map<Integer, String> row = rows.get(i);
            boolean matched = true;
            for (String header : requiredHeaders) {
                if (!containsCell(row, header)) {
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

    private static Map<String, Integer> buildHeaderIndex(Map<Integer, String> headerRow) {
        Map<String, Integer> index = new HashMap<>();
        if (headerRow == null) {
            return index;
        }
        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            String key = normalize(entry.getValue());
            if (StringUtils.hasText(key)) {
                index.putIfAbsent(key, entry.getKey());
            }
        }
        return index;
    }

    private static boolean containsCell(Map<Integer, String> row, String expected) {
        if (row == null) {
            return false;
        }
        String normalized = normalize(expected);
        for (String value : row.values()) {
            if (normalized.equals(normalize(value))) {
                return true;
            }
        }
        return false;
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

    private static String get(Map<Integer, String> row, Integer col) {
        if (row == null || col == null) {
            return null;
        }
        return normalize(row.get(col));
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("\u00A0", " ").trim();
    }
}
