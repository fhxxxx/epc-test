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
        String normalized = raw.trim()
                .replace(",", "")
                .replace("，", "");
        if (normalized.isEmpty()) {
            return null;
        }
        if ("-".equals(normalized)) {
            return BigDecimal.ZERO;
        }
        boolean percent = normalized.endsWith("%");
        String numeric = percent ? normalized.substring(0, normalized.length() - 1).trim() : normalized;
        if (numeric.isEmpty() || "-".equals(numeric)) {
            return BigDecimal.ZERO;
        }
        try {
            BigDecimal value = new BigDecimal(numeric);
            return percent ? value.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP) : value;
        } catch (Exception ignore) {
            return null;
        }
    }

    public static boolean isInvalidNumericText(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim()
                .replace(",", "")
                .replace("，", "");
        if (normalized.isEmpty() || "-".equals(normalized)) {
            return false;
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
}
