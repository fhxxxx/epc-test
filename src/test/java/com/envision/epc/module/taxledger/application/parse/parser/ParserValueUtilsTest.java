package com.envision.epc.module.taxledger.application.parse.parser;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ParserValueUtilsTest {
    @Test
    void shouldUseSameNumericRuleAsNormalizeUtils() {
        assertEquals(new BigDecimal("391472849.34"), ParserValueUtils.toBigDecimal("391472849.34"));
        assertEquals(new BigDecimal("3914729.34"), ParserValueUtils.toBigDecimal("3,914,729.34"));
        assertEquals(BigDecimal.ZERO, ParserValueUtils.toBigDecimal("-"));
        assertEquals(new BigDecimal("0.125000000000"), ParserValueUtils.toBigDecimal("12.5%"));
    }

    @Test
    void shouldReturnNullForInvalidText() {
        assertNull(ParserValueUtils.toBigDecimal("abc"));
    }
}
