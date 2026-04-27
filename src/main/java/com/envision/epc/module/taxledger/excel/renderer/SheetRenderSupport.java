package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Sheet 渲染工具方法。
 */
public final class SheetRenderSupport {
    private SheetRenderSupport() {
    }

    public static void renderPayload(Workbook workbook, String sheetName, String displayName, Object payload) {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(sheetName);
        sheet.getCells().get(0, 0).putValue(displayName);

        if (payload == null) {
            sheet.getCells().get(1, 0).putValue("无数据");
            return;
        }

        if (payload instanceof List<?> listPayload) {
            renderList(sheet, listPayload);
            return;
        }
        renderObject(sheet, payload);
    }

    public static void copySourceSheet(Workbook workbook,
                                       String targetBaseSheetName,
                                       String displayName,
                                       Workbook sourceWorkbook,
                                       String sourceSheetName) {
        if (sourceWorkbook == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "源sheet数据为空: " + displayName);
        }
        Worksheet source = sourceWorkbook.getWorksheets().get(sourceSheetName);
        if (source == null) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "源sheet不存在: " + displayName + " - " + sourceSheetName);
        }

        int index = workbook.getWorksheets().add();
        Worksheet target = workbook.getWorksheets().get(index);
        try {
            target.copy(source);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.BAD_REQUEST,
                    "复制源sheet失败: " + displayName + " - " + sourceSheetName);
        }
        target.setName(resolveSheetName(workbook, targetBaseSheetName, index));
    }

    private static void renderList(Worksheet sheet, List<?> listPayload) {
        if (listPayload.isEmpty()) {
            sheet.getCells().get(1, 0).putValue("无数据");
            return;
        }
        Object first = firstNonNull(listPayload);
        if (first == null) {
            sheet.getCells().get(1, 0).putValue("无数据");
            return;
        }

        List<Field> fields = declaredFields(first.getClass());
        if (fields.isEmpty()) {
            sheet.getCells().get(1, 0).putValue("无可写字段");
            return;
        }

        for (int c = 0; c < fields.size(); c++) {
            sheet.getCells().get(1, c).putValue(fields.get(c).getName());
        }

        int row = 2;
        for (Object item : listPayload) {
            if (item == null) {
                row++;
                continue;
            }
            for (int c = 0; c < fields.size(); c++) {
                sheet.getCells().get(row, c).putValue(readFieldValue(item, fields.get(c)));
            }
            row++;
        }
    }

    private static void renderObject(Worksheet sheet, Object payload) {
        List<Field> fields = declaredFields(payload.getClass());
        if (fields.isEmpty()) {
            sheet.getCells().get(1, 0).putValue("无可写字段");
            return;
        }
        sheet.getCells().get(1, 0).putValue("字段");
        sheet.getCells().get(1, 1).putValue("值");

        int row = 2;
        for (Field field : fields) {
            sheet.getCells().get(row, 0).putValue(field.getName());
            sheet.getCells().get(row, 1).putValue(readFieldValue(payload, field));
            row++;
        }
    }

    private static Object firstNonNull(List<?> values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static List<Field> declaredFields(Class<?> clazz) {
        Field[] declared = clazz.getDeclaredFields();
        List<Field> fields = new ArrayList<>(declared.length);
        for (Field field : declared) {
            int modifiers = field.getModifiers();
            if (Modifier.isStatic(modifiers) || field.isSynthetic()) {
                continue;
            }
            field.setAccessible(true);
            fields.add(field);
        }
        return fields;
    }

    private static String readFieldValue(Object source, Field field) {
        try {
            Object value = field.get(source);
            return value == null ? "" : String.valueOf(value);
        } catch (IllegalAccessException ex) {
            return "";
        }
    }

    private static String resolveSheetName(Workbook workbook, String baseName, int currentIndex) {
        if (workbook.getWorksheets().get(baseName) == null
                || workbook.getWorksheets().get(baseName).getIndex() == currentIndex) {
            return baseName;
        }
        int suffix = 1;
        while (workbook.getWorksheets().get(baseName + "_" + suffix) != null) {
            suffix++;
        }
        return baseName + "_" + suffix;
    }
}
