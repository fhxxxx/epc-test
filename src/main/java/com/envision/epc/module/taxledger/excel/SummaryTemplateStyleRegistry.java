package com.envision.epc.module.taxledger.excel;

import com.aspose.cells.Name;
import com.aspose.cells.Range;
import com.aspose.cells.Workbook;
import com.envision.epc.infrastructure.response.BizException;
import com.envision.epc.infrastructure.response.ErrorCode;

import java.util.EnumMap;
import java.util.Map;

/**
 * Summary 模板命名区域注册表（Name -> RowSpec -> ResolvedRow）。
 */
public final class SummaryTemplateStyleRegistry {
    private final Map<SummaryTemplateNamespace, ResolvedTemplateRow> resolvedRows;

    private SummaryTemplateStyleRegistry(Map<SummaryTemplateNamespace, ResolvedTemplateRow> resolvedRows) {
        this.resolvedRows = resolvedRows;
    }

    public static SummaryTemplateStyleRegistry fromTemplate(Workbook templateWorkbook,
                                                            String templatePath,
                                                            String templateSheet,
                                                            SummaryTemplateRowSpec... specs) {
        Map<SummaryTemplateNamespace, ResolvedTemplateRow> resolved = new EnumMap<>(SummaryTemplateNamespace.class);
        if (specs == null) {
            return new SummaryTemplateStyleRegistry(resolved);
        }
        for (SummaryTemplateRowSpec spec : specs) {
            if (spec == null) {
                continue;
            }
            Name named = templateWorkbook.getWorksheets().getNames().get(spec.namedRangeName());
            if (named == null) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "summary named range missing, template=" + templatePath
                                + ", sheet=" + templateSheet
                                + ", namespace=" + spec.namespace()
                                + ", namedRange=" + spec.namedRangeName());
            }
            Range range = named.getRange();
            if (range == null) {
                throw new BizException(ErrorCode.BAD_REQUEST,
                        "summary named range invalid, template=" + templatePath
                                + ", sheet=" + templateSheet
                                + ", namespace=" + spec.namespace()
                                + ", namedRange=" + spec.namedRangeName());
            }
            resolved.put(spec.namespace(), new ResolvedTemplateRow(
                    spec,
                    range.getWorksheet().getName(),
                    range.getFirstRow(),
                    range.getFirstColumn(),
                    range.getColumnCount()
            ));
        }
        return new SummaryTemplateStyleRegistry(resolved);
    }

    public ResolvedTemplateRow get(SummaryTemplateNamespace namespace) {
        ResolvedTemplateRow row = resolvedRows.get(namespace);
        if (row == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "summary namespace unresolved: " + namespace);
        }
        return row;
    }

    public record ResolvedTemplateRow(
            SummaryTemplateRowSpec spec,
            String sheetName,
            int rowIndex,
            int firstColumn,
            int columnCount
    ) {
    }
}

