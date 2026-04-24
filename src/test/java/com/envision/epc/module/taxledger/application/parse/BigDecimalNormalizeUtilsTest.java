package com.envision.epc.module.taxledger.application.parse;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BigDecimalNormalizeUtilsTest {
    @Test
    void shouldParseStandardNumber() {
        assertEquals(new BigDecimal("391472849.34"), BigDecimalNormalizeUtils.parse("391472849.34"));
    }

    @Test
    void shouldParseThousandsSeparatedNumber() {
        assertEquals(new BigDecimal("3914729.34"), BigDecimalNormalizeUtils.parse("3,914,729.34"));
        assertEquals(new BigDecimal("3914729.34"), BigDecimalNormalizeUtils.parse("3，914，729.34"));
    }

    @Test
    void shouldTreatDashAsZero() {
        assertEquals(BigDecimal.ZERO, BigDecimalNormalizeUtils.parse("-"));
        assertEquals(BigDecimal.ZERO, BigDecimalNormalizeUtils.parse(" - "));
    }

    @Test
    void shouldParsePercentToDecimal() {
        assertEquals(new BigDecimal("0.125000000000"), BigDecimalNormalizeUtils.parse("12.5%"));
    }

    @Test
    void shouldReturnNullForBlankOrInvalidText() {
        assertNull(BigDecimalNormalizeUtils.parse(""));
        assertNull(BigDecimalNormalizeUtils.parse("   "));
        assertNull(BigDecimalNormalizeUtils.parse("abc"));
    }
}
