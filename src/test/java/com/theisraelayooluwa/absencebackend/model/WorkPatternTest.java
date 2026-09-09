package com.theisraelayooluwa.absencebackend.model;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkPatternTest {

    private static WorkPattern fiveDayFullTimePattern() {
        WorkPattern pattern = new WorkPattern();
        Map<DayOfWeek, Double> days = new EnumMap<>(DayOfWeek.class);
        days.put(DayOfWeek.MONDAY, 8.0);
        days.put(DayOfWeek.TUESDAY, 8.0);
        days.put(DayOfWeek.WEDNESDAY, 8.0);
        days.put(DayOfWeek.THURSDAY, 8.0);
        days.put(DayOfWeek.FRIDAY, 8.0);
        pattern.setDayHours(days);
        return pattern;
    }

    @Test
    void daysPerWeek_countsOnlyDaysWithPositiveHours() {
        assertEquals(5, fiveDayFullTimePattern().getDaysPerWeek());
    }

    @Test
    void daysPerWeek_ignoresDaysExplicitlyRecordedAsZeroHours() {
        WorkPattern pattern = fiveDayFullTimePattern();
        pattern.getDayHours().put(DayOfWeek.SATURDAY, 0.0);

        assertEquals(5, pattern.getDaysPerWeek());
    }

    @Test
    void totalWeeklyHours_sumsAcrossAllRecordedDays() {
        // Uneven pattern from the briefing example: Tue=6h, Wed=8h, Thu=8h.
        WorkPattern pattern = new WorkPattern();
        Map<DayOfWeek, Double> days = new EnumMap<>(DayOfWeek.class);
        days.put(DayOfWeek.TUESDAY, 6.0);
        days.put(DayOfWeek.WEDNESDAY, 8.0);
        days.put(DayOfWeek.THURSDAY, 8.0);
        pattern.setDayHours(days);

        assertEquals(22.0, pattern.getTotalWeeklyHours());
    }

    @Test
    void fullTimeEquivalentFraction_isProportionOfStandardWeek() {
        WorkPattern halfPattern = new WorkPattern();
        Map<DayOfWeek, Double> days = new EnumMap<>(DayOfWeek.class);
        days.put(DayOfWeek.MONDAY, 8.0);
        days.put(DayOfWeek.TUESDAY, 8.0);
        days.put(DayOfWeek.WEDNESDAY, 4.0);
        halfPattern.setDayHours(days);

        assertEquals(0.5, halfPattern.getFullTimeEquivalentFraction(40.0));
    }

    @Test
    void fullTimeEquivalentFraction_defaultsToOneWhenStandardWeekIsUnset() {
        assertEquals(1.0, fiveDayFullTimePattern().getFullTimeEquivalentFraction(0.0));
    }
}
