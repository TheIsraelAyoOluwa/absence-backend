package com.theisraelayooluwa.absencebackend.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Each policy is exercised at a value that is NOT already on a clean boundary,
 * so the test actually distinguishes "rounds correctly" from "happened to pass
 * because the input needed no rounding" (see the ROUND_NEAREST_HALF_HOUR enum
 * constant name history: it used to be misleadingly called ROUND_NEAREST_HALF_DAY
 * despite always operating on hours).
 */
class RoundingPolicyTest {

    private static void assertBigDecimalEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void roundUp_pushesAnyFractionalRemainderToTheNextTenth() {
        assertBigDecimalEquals("3.2", RoundingPolicy.ROUND_UP.apply(new BigDecimal("3.11")));
        assertBigDecimalEquals("3.2", RoundingPolicy.ROUND_UP.apply(new BigDecimal("3.20")));
    }

    @Test
    void roundDown_truncatesAnyFractionalRemainder() {
        assertBigDecimalEquals("3.1", RoundingPolicy.ROUND_DOWN.apply(new BigDecimal("3.19")));
        assertBigDecimalEquals("3.2", RoundingPolicy.ROUND_DOWN.apply(new BigDecimal("3.20")));
    }

    @Test
    void roundNearestHalfHour_snapsToTheClosestHalfHourIncrement() {
        assertBigDecimalEquals("3.0", RoundingPolicy.ROUND_NEAREST_HALF_HOUR.apply(new BigDecimal("3.24")));
        assertBigDecimalEquals("3.5", RoundingPolicy.ROUND_NEAREST_HALF_HOUR.apply(new BigDecimal("3.26")));
    }

    @Test
    void roundNearestHalfHour_exactMidpointRoundsUp() {
        // 3.25 is exactly between 3.0 and 3.5; HALF_UP must break the tie upward.
        assertBigDecimalEquals("3.5", RoundingPolicy.ROUND_NEAREST_HALF_HOUR.apply(new BigDecimal("3.25")));
    }

    @Test
    void noRounding_onlyFixesTheDisplayScaleWithoutAlteringTheValue() {
        assertBigDecimalEquals("3.1416", RoundingPolicy.NO_ROUNDING.apply(new BigDecimal("3.14159")));
    }
}
