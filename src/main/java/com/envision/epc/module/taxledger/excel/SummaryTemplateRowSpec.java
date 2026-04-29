package com.envision.epc.module.taxledger.excel;

/**
 * Summary 模板行规范定义。
 */
public record SummaryTemplateRowSpec(
        SummaryTemplateNamespace namespace,
        String namedRangeName,
        boolean copyMerge,
        int fillColumnStart,
        int fillColumnEnd
) {
    public static SummaryTemplateRowSpec of(SummaryTemplateNamespace namespace,
                                            String namedRangeName,
                                            int fillColumnStart,
                                            int fillColumnEnd) {
        return new SummaryTemplateRowSpec(namespace, namedRangeName, true, fillColumnStart, fillColumnEnd);
    }
}

