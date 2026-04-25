package com.envision.epc.module.taxledger.application.parse.parser;

import com.alibaba.excel.read.builder.ExcelReaderBuilder;
import com.alibaba.excel.read.builder.ExcelReaderSheetBuilder;
import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.aspose.cells.WorksheetCollection;
import com.envision.epc.module.taxledger.domain.FileCategoryEnum;
import org.springframework.util.StringUtils;

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
        String targetSheetName = category.getTargetSheetName();
        WorksheetCollection worksheets = workbook.getWorksheets();
        if (!StringUtils.hasText(targetSheetName)) {
            return worksheets.get(0);
        }

        Worksheet exactMatch = worksheets.get(targetSheetName);
        if (exactMatch != null) {
            return exactMatch;
        }

        for (int i = 0; i < worksheets.getCount(); i++) {
            Worksheet sheet = worksheets.get(i);
            if (sheet != null && targetSheetName.trim().equals(sheet.getName().trim())) {
                return sheet;
            }
        }
        throw new IllegalArgumentException("sheet not found: " + targetSheetName);
    }
}
