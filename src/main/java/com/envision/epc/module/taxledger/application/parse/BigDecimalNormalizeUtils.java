package com.envision.epc.module.taxledger.application.parse;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal 文本归一化工具。
 */
public final class BigDecimalNormalizeUtils {
    private BigDecimalNormalizeUtils() {
    }

    public static BigDecimal parse(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = normalizeNumericText(raw);
        if (normalized.isEmpty()) {
            return null;
        }
        if ("-".equals(normalized)) {
            return BigDecimal.ZERO;
        }
        boolean negativeByParentheses = normalized.startsWith("(") && normalized.endsWith(")");
        if (negativeByParentheses) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        boolean percent = normalized.endsWith("%");
        String numeric = percent ? normalized.substring(0, normalized.length() - 1).trim() : normalized;
        if (numeric.isEmpty() || "-".equals(numeric)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal value = new BigDecimal(numeric);
            if (negativeByParentheses) {
                value = value.negate();
            }
            return percent ? value.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP) : value;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static boolean isInvalidNumericText(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = normalizeNumericText(raw);
        if (normalized.isEmpty() || "-".equals(normalized)) {
            return false;
        }
        if (normalized.startsWith("(") && normalized.endsWith(")")) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        String numeric = normalized.endsWith("%")
                ? normalized.substring(0, normalized.length() - 1).trim()
                : normalized;
        if (numeric.isEmpty() || "-".equals(numeric)) {
            return false;
        }
        try {
            new BigDecimal(numeric);
            return false;
        } catch (Exception ignore) {
            return true;
        }
    }

    private static String normalizeNumericText(String raw) {
        return raw.trim()
                // 会计格式可能包含的各种空白字符
                .replace("\u00A0", "")
                .replace("\u2007", "")
                .replace("\u202F", "")
                .replace(" ", "")
                // 各类减号统一成 ASCII '-'
                .replace('−', '-')
                .replace('—', '-')
                .replace('–', '-')
                .replace('﹣', '-')
                .replace('－', '-')
                // 千分位符
                .replace(",", "")
                .replace("，", "");
    }
}
