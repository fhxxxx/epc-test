package com.envision.epc.module.taxledger.application.parse.parser;

import java.math.BigDecimal;

public final class ParserValueUtils {
    private ParserValueUtils() {
    }

    public static BigDecimal toBigDecimal(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim()
                .replace(",", "")
                .replace("，", "");
        if (normalized.isBlank() || "-".equals(normalized)) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (Exception ignore) {
            return null;
        }
    }
}

