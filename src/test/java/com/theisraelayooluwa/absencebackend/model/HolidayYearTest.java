package com.theisraelayooluwa.absencebackend.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HolidayYearTest {

    private static HolidayYear yearFrom(LocalDate start, LocalDate end) {
        HolidayYear holidayYear = new HolidayYear();
        holidayYear.setStartDate(start);
        holidayYear.setEndDate(end);
        return holidayYear;
    }

    @Test
    void lengthInDays_isThreeHundredAndSixtyFiveForAnOrdinaryYear() {
        HolidayYear holidayYear = yearFrom(LocalDate.of(2025, 1, 1), LocalDate.of(2025, 12, 31));
        assertEquals(365, holidayYear.lengthInDays());
        assertFalse(holidayYear.isLeapYear());
    }

    @Test
    void lengthInDays_accountsForTheLeapDay() {
        HolidayYear holidayYear = yearFrom(LocalDate.of(2028, 1, 1), LocalDate.of(2028, 12, 31));
        assertEquals(366, holidayYear.lengthInDays());
        assertTrue(holidayYear.isLeapYear());
    }

    @Test
    void contains_isInclusiveOfBothBoundaryDates() {
        HolidayYear holidayYear = yearFrom(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));

        assertTrue(holidayYear.contains(LocalDate.of(2026, 4, 1)));
        assertTrue(holidayYear.contains(LocalDate.of(2027, 3, 31)));
        assertTrue(holidayYear.contains(LocalDate.of(2026, 9, 15)));
    }

    @Test
    void contains_isFalseOutsideTheWindow() {
        HolidayYear holidayYear = yearFrom(LocalDate.of(2026, 4, 1), LocalDate.of(2027, 3, 31));

        assertFalse(holidayYear.contains(LocalDate.of(2026, 3, 31)));
        assertFalse(holidayYear.contains(LocalDate.of(2027, 4, 1)));
    }
}
