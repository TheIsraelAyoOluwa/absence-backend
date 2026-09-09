package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import com.theisraelayooluwa.absencebackend.model.RoundingPolicy;
import com.theisraelayooluwa.absencebackend.model.Term;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the entitlement rules described on briefing slides 6, 7, 10, 11 and 12:
 * proration by day-overlap, rounding policy application, carry-over capping,
 * extra holiday, and forfeiture of unused entitlement.
 */
class EntitlementServiceTest {

    private final EntitlementService entitlementService = new EntitlementService();

    private static Term term(LocalDate start, LocalDate end, double annualHours, RoundingPolicy policy) {
        Term term = new Term();
        term.setStartDate(start);
        term.setEndDate(end);
        term.setAnnualEntitlementHours(annualHours);
        term.setRoundingPolicy(policy);
        return term;
    }

    private static HolidayYear holidayYear(LocalDate start, LocalDate end) {
        HolidayYear holidayYear = new HolidayYear();
        holidayYear.setStartDate(start);
        holidayYear.setEndDate(end);
        return holidayYear;
    }

    private static void assertBigDecimalEquals(double expected, BigDecimal actual) {
        assertEquals(0, BigDecimal.valueOf(expected).compareTo(actual),
                () -> "expected " + expected + " but was " + actual);
    }

    @Test
    void proratedEntitlement_termCoversWholeHolidayYear_returnsFullEntitlement() {
        // 2020 is a leap year (366 days); term runs the entire holiday year.
        HolidayYear holidayYear = holidayYear(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
        Term term = term(LocalDate.of(2020, 1, 1), null, 224.0, RoundingPolicy.NO_ROUNDING);

        BigDecimal result = entitlementService.calculateProratedEntitlement(term, holidayYear);

        assertBigDecimalEquals(224.0, result);
    }

    @Test
    void proratedEntitlement_termStartsMidYear_isProratedByDayOverlap() {
        // Non-leap 2025 holiday year (365 days); term starts 1 July, still active (endDate null).
        HolidayYear holidayYear = holidayYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        // 2920 hours / 365 days = 8 hours/day, so the maths comes out exact for the assertion.
        Term term = term(LocalDate.of(2025, 7, 1), null, 2920.0, RoundingPolicy.NO_ROUNDING);

        BigDecimal result = entitlementService.calculateProratedEntitlement(term, holidayYear);

        // 1 Jul - 31 Dec 2025 inclusive = 184 days; 184 * 8 = 1472.
        assertBigDecimalEquals(1472.0, result);
    }

    @Test
    void proratedEntitlement_termEndsBeforeHolidayYearStarts_returnsZero() {
        HolidayYear holidayYear = holidayYear(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        Term term = term(LocalDate.of(2026, 1, 10), null, 224.0, RoundingPolicy.NO_ROUNDING);

        BigDecimal result = entitlementService.calculateProratedEntitlement(term, holidayYear);

        assertEquals(0, BigDecimal.ZERO.compareTo(result));
    }

    @Test
    void proratedEntitlement_roundUpPolicy_roundsFractionalHoursUp() {
        HolidayYear holidayYear = holidayYear(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31)); // 365 days
        // 100 hours prorated over 100/365 of the year -> a small, non-round fractional figure.
        Term term = term(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 5), 100.0, RoundingPolicy.ROUND_UP);

        BigDecimal result = entitlementService.calculateProratedEntitlement(term, holidayYear);

        // 5 days / 365 * 100 = 1.36986... which ROUND_UP must push up to the next tenth: 1.4
        assertBigDecimalEquals(1.4, result);
    }

    @Test
    void proratedEntitlement_roundDownPolicy_truncatesFractionalHours() {
        HolidayYear holidayYear = holidayYear(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 12, 31));
        Term term = term(LocalDate.of(2021, 1, 1), LocalDate.of(2021, 1, 5), 100.0, RoundingPolicy.ROUND_DOWN);

        BigDecimal result = entitlementService.calculateProratedEntitlement(term, holidayYear);

        // Same 1.36986... but ROUND_DOWN must floor to 1.3
        assertBigDecimalEquals(1.3, result);
    }

    @Test
    void totalAvailableHours_addsCappedCarryOverAndExtraHolidayToProration() {
        HolidayYear holidayYear = holidayYear(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
        holidayYear.setCarryOverLimitHours(40.0);
        holidayYear.setCarriedOverHours(60.0); // exceeds the cap - must be clamped to 40
        holidayYear.setExtraHolidayHours(8.0);
        Term term = term(LocalDate.of(2020, 1, 1), null, 224.0, RoundingPolicy.NO_ROUNDING);

        BigDecimal result = entitlementService.calculateTotalAvailableHours(term, holidayYear);

        // 224 (full prorated entitlement) + 40 (capped carry-over, not 60) + 8 (extra) = 272
        assertBigDecimalEquals(272.0, result);
    }

    @Test
    void totalAvailableHours_carryOverBelowCap_isUsedInFull() {
        HolidayYear holidayYear = holidayYear(LocalDate.of(2020, 1, 1), LocalDate.of(2020, 12, 31));
        holidayYear.setCarryOverLimitHours(40.0);
        holidayYear.setCarriedOverHours(10.0);
        holidayYear.setExtraHolidayHours(0.0);
        Term term = term(LocalDate.of(2020, 1, 1), null, 224.0, RoundingPolicy.NO_ROUNDING);

        BigDecimal result = entitlementService.calculateTotalAvailableHours(term, holidayYear);

        assertBigDecimalEquals(234.0, result);
    }

    @Test
    void forfeitedEntitlement_isDifferenceBetweenFullYearAndProratedAmount() {
        BigDecimal forfeited = entitlementService.calculateForfeitedEntitlement(
                BigDecimal.valueOf(224.0), BigDecimal.valueOf(150.0));

        assertBigDecimalEquals(74.0, forfeited);
    }

    @Test
    void forfeitedEntitlement_neverGoesNegative() {
        // Prorated exceeding the "full year" figure (e.g. after a mid-year entitlement increase)
        // must not produce a negative forfeiture.
        BigDecimal forfeited = entitlementService.calculateForfeitedEntitlement(
                BigDecimal.valueOf(224.0), BigDecimal.valueOf(250.0));

        assertEquals(0, BigDecimal.ZERO.compareTo(forfeited));
    }
}
