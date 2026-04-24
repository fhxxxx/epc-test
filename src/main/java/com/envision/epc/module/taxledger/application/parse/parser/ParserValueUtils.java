package com.envision.epc.module.taxledger.application.parse.parser;

import com.envision.epc.module.taxledger.application.parse.BigDecimalNormalizeUtils;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
public final class ParserValueUtils {
    private ParserValueUtils() {
    }

    public static BigDecimal toBigDecimal(String value) {
        BigDecimal parsed = BigDecimalNormalizeUtils.parse(value);
        if (parsed == null && BigDecimalNormalizeUtils.isInvalidNumericText(value)) {
            log.warn("INVALID_NUMERIC_TEXT: value={}", value);
        }
        return parsed;
    }
}
