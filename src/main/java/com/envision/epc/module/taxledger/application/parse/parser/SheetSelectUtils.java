package com.envision.epc.module.taxledger.application.parse.parser;

import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.builder.ExcelReaderSheetBuilder;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.aspose.cells.WorksheetCollection;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.util.Locale;

/**
 * 统一的 sheet 选择工具。
 * 优先使用 FileCategoryEnum 配置的 targetSheetName；
 * 未配置时回退到第一个 sheet。
 */
public final class SheetSelectUtils {
    private SheetSelectUtils() {
    }

    public static ExcelReaderSheetBuilder resolveEasyExcelSheet(ExcelReaderBuilder readerBuilder,
                                                                FileCategoryEnum category) {
        String targetSheetName = category.getTargetSheetName();
        if (!StringUtils.hasText(targetSheetName)) {
            return readerBuilder.sheet();
        }
        return readerBuilder.sheet(targetSheetName);
    }

    public static Worksheet resolveAsposeSheet(Workbook workbook, FileCategoryEnum category) {
        return resolveSheet(workbook.getWorksheets(), category);
    }

    public static String resolveEasyExcelSheetName(byte[] bytes, FileCategoryEnum category) {
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("workbook bytes is empty");
        }
        try {
            Workbook workbook = new Workbook(new ByteArrayInputStream(bytes));
            Worksheet sheet = resolveSheet(workbook.getWorksheets(), category);
            return sheet == null ? null : sheet.getName();
        } catch (Exception e) {
            throw new IllegalArgumentException("resolve sheet name failed: " + e.getMessage(), e);
        }
    }

    private static Worksheet resolveSheet(WorksheetCollection worksheets, FileCategoryEnum category) {
        if (worksheets == null || worksheets.getCount() <= 0) {
            throw new IllegalArgumentException("sheet not found: workbook has no sheets");
        }
        if (worksheets.getCount() == 1) {
            return worksheets.get(0);
        }

        String targetSheetName = category == null ? null : category.getTargetSheetName();
        if (!StringUtils.hasText(targetSheetName)) {
            return worksheets.get(0);
        }

        Worksheet exactMatch = worksheets.get(targetSheetName);
        if (exactMatch != null) {
            return exactMatch;
        }

        String normalizedTarget = normalizeSheetName(targetSheetName);
        for (int i = 0; i < worksheets.getCount(); i++) {
            Worksheet sheet = worksheets.get(i);
            if (sheet != null && normalizedTarget.equals(normalizeSheetName(sheet.getName()))) {
                return sheet;
            }
        }

        for (int i = 0; i < worksheets.getCount(); i++) {
            Worksheet sheet = worksheets.get(i);
            if (sheet == null || !StringUtils.hasText(sheet.getName())) {
                continue;
            }
            String normalizedName = normalizeSheetName(sheet.getName());
            if (normalizedName.contains(normalizedTarget) || normalizedTarget.contains(normalizedName)) {
                return sheet;
            }
        }
        throw new IllegalArgumentException("sheet not found: " + targetSheetName);
    }

    private static String normalizeSheetName(String name) {
        if (!StringUtils.hasText(name)) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
