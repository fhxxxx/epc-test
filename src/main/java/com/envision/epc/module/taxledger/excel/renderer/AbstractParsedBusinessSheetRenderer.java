package com.envision.epc.module.taxledger.excel.renderer;

import com.aspose.cells.Workbook;
import com.aspose.cells.Worksheet;
import com.envision.epc.module.taxledger.application.ledger.LedgerRenderContext;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetCode;
import com.envision.epc.module.taxledger.application.ledger.LedgerSheetRenderer;
import com.envision.epc.module.taxledger.application.ledger.data.BusinessSheetData;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * 真实业务 Sheet 通用渲染器（结构化数据写入）
 */
public abstract class AbstractParsedBusinessSheetRenderer<T extends BusinessSheetData.ParsedSheetData<?>>
        implements LedgerSheetRenderer<T> {
    protected abstract LedgerSheetCode code();

    @Override
    public LedgerSheetCode support() {
        return code();
    }

    @Override
    public void render(Workbook workbook, T data, LedgerRenderContext ctx) {
        int index = workbook.getWorksheets().add();
        Worksheet sheet = workbook.getWorksheets().get(index);
        sheet.setName(data.sheetCode().getSheetName());
        sheet.getCells().get(0, 0).putValue(data.sheetCode().getDisplayName());

        Object payload = data.getPayload();
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

    private void renderList(Worksheet sheet, List<?> listPayload) {
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

    private void renderObject(Worksheet sheet, Object payload) {
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

    private Object firstNonNull(List<?> values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private List<Field> declaredFields(Class<?> clazz) {
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

    private String readFieldValue(Object source, Field field) {
        try {
            Object value = field.get(source);
            return value == null ? "" : String.valueOf(value);
        } catch (IllegalAccessException ex) {
            return "";
        }
    }
}
