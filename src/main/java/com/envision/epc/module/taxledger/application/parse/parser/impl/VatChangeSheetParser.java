package com.envision.epc.module.taxledger.application.parse.parser.impl;

import com.aspose.cells.Cell;
import com.aspose.cells.Cells;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.dto.VatChangeRowDTO;
import com.envision.epc.module.taxledger.application.parse.ParseContext;
import com.envision.epc.module.taxledger.application.parse.ParseResult;
import com.envision.epc.module.taxledger.application.parse.parser.ParserValueUtils;
import com.envision.epc.module.taxledger.application.parse.parser.SheetParser;
import com.envision.epc.module.taxledger.application.parse.parser.SheetSelectUtils;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class VatChangeSheetParser implements SheetParser<List<VatChangeRowDTO>> {
    private static final int BLANK_ROW_BREAK_THRESHOLD = 5;
    private static final Pattern CN_PAREN_PATTERN = Pattern.compile("（([^）]+)）");
    private static final Pattern EN_PAREN_PATTERN = Pattern.compile("\\(([^)]+)\\)");

    @Override
    public FileCategoryEnum category() {
        return FileCategoryEnum.VAT_CHANGE;
    }

    @Override
    public Class<List<VatChangeRowDTO>> resultType() {
        @SuppressWarnings("unchecked")
        Class<List<VatChangeRowDTO>> cls = (Class<List<VatChangeRowDTO>>) (Class<?>) List.class;
        return cls;
    }

    @Override
    public ParseResult<List<VatChangeRowDTO>> parse(InputStream inputStream, ParseContext context) {
        ParseResult<List<VatChangeRowDTO>> result = ParseResult.<List<VatChangeRowDTO>>builder()
                .data(new ArrayList<>())
                .build();
        try {
            Workbook workbook = new Workbook(inputStream);
            Worksheet sheet = SheetSelectUtils.resolveAsposeSheet(workbook, category());
            Cells cells = sheet.getCells();
            int maxRow = cells.getMaxDataRow();
            int maxCol = cells.getMaxDataColumn();

            int headerRow = findHeaderRow(cells, maxRow, maxCol);
            if (headerRow < 0) {
                result.addIssue("INVALID_WORKBOOK: 增值税变动表主表金额表头缺失（需至少识别合计+两列金额列）");
                return result;
            }

            int baseItemCol = findCol(cells, headerRow, maxCol, "基础条目");
            int splitBasisCol = findCol(cells, headerRow, maxCol, "拆分依据");
            int itemNameCol = findCol(cells, headerRow, maxCol, "条目");
            int unbilledCol = findCol(cells, headerRow, maxCol, "未开票金额");
            int currentInvoicedCol = findCol(cells, headerRow, maxCol, "当月开票金额");
            int prevInvoicedCol = findCol(cells, headerRow, maxCol, "以前月度开票金额");
            int totalCol = findCol(cells, headerRow, maxCol, "合计");

            if (itemNameCol < 0) {
                if (unbilledCol > 0) {
                    itemNameCol = unbilledCol - 1;
                } else if (totalCol >= 3) {
                    itemNameCol = totalCol - 3;
                }
            }
            if (totalCol < 0) {
                result.addIssue("INVALID_WORKBOOK: 增值税变动表主表金额表头缺失（合计）");
                return result;
            }

            int blankRows = 0;
            for (int row = headerRow + 1; row <= maxRow; row++) {
                String itemName = getIfValid(cells, row, itemNameCol);
                String baseItemRaw = getIfValid(cells, row, baseItemCol);
                String splitBasisRaw = getIfValid(cells, row, splitBasisCol);
                java.math.BigDecimal unbilled = parseIfValid(cells, row, unbilledCol);
                java.math.BigDecimal currentInvoiced = parseIfValid(cells, row, currentInvoicedCol);
                java.math.BigDecimal prevInvoiced = parseIfValid(cells, row, prevInvoicedCol);
                java.math.BigDecimal total = parseIfValid(cells, row, totalCol);

                boolean hasAmount = unbilled != null || currentInvoiced != null || prevInvoiced != null || total != null;
                boolean hasItemName = StringUtils.hasText(itemName);
                if (!hasItemName && !hasAmount) {
                    blankRows++;
                    if (blankRows >= BLANK_ROW_BREAK_THRESHOLD) {
                        break;
                    }
                    continue;
                }
                blankRows = 0;

                // 真实台账按“条目列”驱动解析：条目为空则跳过（即使金额列有噪声）
                if (!hasItemName) {
                    continue;
                }

                VatChangeRowDTO dto = new VatChangeRowDTO();
                String normalizedItemName = normalize(itemName);
                dto.setItemName(StringUtils.hasText(normalizedItemName) ? normalizedItemName : normalize(baseItemRaw));
                dto.setBaseItem(StringUtils.hasText(baseItemRaw) ? normalize(baseItemRaw) : deriveBaseItem(normalizedItemName));
                dto.setSplitBasis(StringUtils.hasText(splitBasisRaw) ? normalize(splitBasisRaw) : extractSplitBasis(normalizedItemName));
                dto.setUnbilledAmount(unbilled);
                dto.setCurrentMonthInvoicedAmount(currentInvoiced);
                dto.setPreviousMonthInvoicedAmount(prevInvoiced);
                dto.setTotalAmount(total);
                result.getData().add(dto);
            }
            return result;
        } catch (Exception e) {
            result.addIssue("INVALID_WORKBOOK: " + e.getMessage());
            return result;
        }
    }

    private int findHeaderRow(Cells cells, int maxRow, int maxCol) {
        for (int row = 0; row <= maxRow; row++) {
            int totalCol = findCol(cells, row, maxCol, "合计");
            if (totalCol < 0) {
                continue;
            }
            int hit = 0;
            if (findCol(cells, row, maxCol, "未开票金额") >= 0) {
                hit++;
            }
            if (findCol(cells, row, maxCol, "当月开票金额") >= 0) {
                hit++;
            }
            if (findCol(cells, row, maxCol, "以前月度开票金额") >= 0) {
                hit++;
            }
            if (hit >= 2) {
                return row;
            }
        }
        return -1;
    }

    private int findCol(Cells cells, int row, int maxCol, String... candidates) {
        for (int col = 0; col <= maxCol; col++) {
            String value = normalize(text(cells, row, col));
            for (String candidate : candidates) {
                String target = normalize(candidate);
                if (value.equals(target) || value.contains(target)) {
                    return col;
                }
            }
        }
        return -1;
    }

    private String getIfValid(Cells cells, int row, int col) {
        if (col < 0) {
            return null;
        }
        String value = normalize(text(cells, row, col));
        return StringUtils.hasText(value) ? value : null;
    }

    private java.math.BigDecimal parseIfValid(Cells cells, int row, int col) {
        if (col < 0) {
            return null;
        }
        return ParserValueUtils.toBigDecimal(text(cells, row, col));
    }

    private String text(Cells cells, int row, int col) {
        Cell cell = cells.get(row, col);
        if (cell == null || cell.getValue() == null) {
            return "";
        }
        return cell.getStringValue();
    }

    private String normalize(String text) {
        return text == null ? "" : text.replace("\u00A0", " ").trim();
    }

    private String deriveBaseItem(String itemName) {
        if (!StringUtils.hasText(itemName)) {
            return null;
        }
        String text = normalize(itemName);
        text = text.replaceAll("（[^）]*）", "");
        text = text.replaceAll("\\([^)]*\\)", "");
        text = normalize(text);
        return StringUtils.hasText(text) ? text : null;
    }

    private String extractSplitBasis(String itemName) {
        if (!StringUtils.hasText(itemName)) {
            return null;
        }
        String text = normalize(itemName);
        Matcher cn = CN_PAREN_PATTERN.matcher(text);
        if (cn.find()) {
            String v = normalize(cn.group(1));
            return StringUtils.hasText(v) ? v : null;
        }
        Matcher en = EN_PAREN_PATTERN.matcher(text);
        if (en.find()) {
            String v = normalize(en.group(1));
            return StringUtils.hasText(v) ? v : null;
        }
        return null;
    }

}
