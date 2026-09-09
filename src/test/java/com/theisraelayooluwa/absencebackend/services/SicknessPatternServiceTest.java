package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.model.Absence;
import com.theisraelayooluwa.absencebackend.model.AbsenceStatus;
import com.theisraelayooluwa.absencebackend.model.AbsenceType;
import com.theisraelayooluwa.absencebackend.repository.AbsenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * Covers the Bradford Factor calculation (S^2 x D) surfaced by the Sickness
 * Insights view, plus the supporting day-of-week and summary statistics.
 */
@ExtendWith(MockitoExtension.class)
class SicknessPatternServiceTest {

    @Mock
    private AbsenceRepository absenceRepository;

    private SicknessPatternService sicknessPatternService;

    @BeforeEach
    void setUp() {
        sicknessPatternService = new SicknessPatternService(absenceRepository);
    }

    private static Absence sicknessSpell(LocalDate start, LocalDate end, double durationHours) {
        Absence absence = new Absence();
        absence.setType(AbsenceType.SICKNESS);
        absence.setStatus(AbsenceStatus.RECORDED);
        absence.setStartDate(start);
        absence.setEndDate(end);
        absence.setDurationHours(durationHours);
        return absence;
    }

    @Test
    void bradfordFactor_threeOneDaySpells_scoresDisproportionatelyHigherThanOneLongSpell() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<Absence> threeSpells = List.of(
                sicknessSpell(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5), 8.0),
                sicknessSpell(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 10), 8.0),
                sicknessSpell(LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 2), 8.0)
        );
        when(absenceRepository.findByEmployeeIdAndStartDateBetween(1L, from, to)).thenReturn(threeSpells);

        SicknessPatternService.BradfordFactorResult result =
                sicknessPatternService.calculateBradfordFactor(1L, from, to);

        // S=3 episodes, D=3 days -> 3^2 x 3 = 27
        assertEquals(3, result.episodes());
        assertEquals(3.0, result.totalDays());
        assertEquals(27L, result.bradfordScore());
    }

    @Test
    void bradfordFactor_oneSpellOfEqualTotalLength_scoresMuchLower() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<Absence> oneSpell = List.of(
                sicknessSpell(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 7), 24.0) // one 3-day spell
        );
        when(absenceRepository.findByEmployeeIdAndStartDateBetween(1L, from, to)).thenReturn(oneSpell);

        SicknessPatternService.BradfordFactorResult result =
                sicknessPatternService.calculateBradfordFactor(1L, from, to);

        // S=1 episode, D=3 days -> 1^2 x 3 = 3, versus 27 for the three-spells case above.
        assertEquals(1, result.episodes());
        assertEquals(3.0, result.totalDays());
        assertEquals(3L, result.bradfordScore());
    }

    @Test
    void bradfordFactor_ignoresNonSicknessAbsenceInTheSameWindow() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        Absence holiday = new Absence();
        holiday.setType(AbsenceType.HOLIDAY);
        holiday.setStatus(AbsenceStatus.APPROVED);
        holiday.setStartDate(LocalDate.of(2026, 1, 10));
        holiday.setEndDate(LocalDate.of(2026, 1, 12));
        holiday.setDurationHours(24.0);

        when(absenceRepository.findByEmployeeIdAndStartDateBetween(1L, from, to)).thenReturn(List.of(holiday));

        SicknessPatternService.BradfordFactorResult result =
                sicknessPatternService.calculateBradfordFactor(1L, from, to);

        assertEquals(0, result.episodes());
        assertEquals(0L, result.bradfordScore());
    }

    @Test
    void absencesByDayOfWeek_onlyCountsUnplannedAbsenceAndCoversMultiDaySpells() {
        // Monday 5 Jan - Wednesday 7 Jan 2026: touches Mon, Tue, Wed.
        Absence sickness = sicknessSpell(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 7), 24.0);
        Absence holiday = new Absence();
        holiday.setType(AbsenceType.HOLIDAY); // planned - must be excluded
        holiday.setStatus(AbsenceStatus.APPROVED);
        holiday.setStartDate(LocalDate.of(2026, 1, 12));
        holiday.setEndDate(LocalDate.of(2026, 1, 12));

        when(absenceRepository.findByEmployeeId(1L)).thenReturn(List.of(sickness, holiday));

        var counts = sicknessPatternService.absencesByDayOfWeek(1L);

        assertEquals(1L, counts.get(DayOfWeek.MONDAY));
        assertEquals(1L, counts.get(DayOfWeek.TUESDAY));
        assertEquals(1L, counts.get(DayOfWeek.WEDNESDAY));
        assertEquals(0L, counts.get(DayOfWeek.THURSDAY));
        assertEquals(0L, counts.get(DayOfWeek.MONDAY.plus(6))); // Sunday, sanity check on the default
    }

    @Test
    void calculateStatistics_averagesSpellLengthAcrossAllSpellsInWindow() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        List<Absence> spells = List.of(
                sicknessSpell(LocalDate.of(2026, 1, 5), LocalDate.of(2026, 1, 5), 8.0),  // 1 day
                sicknessSpell(LocalDate.of(2026, 2, 10), LocalDate.of(2026, 2, 12), 24.0) // 3 days
        );
        when(absenceRepository.findByEmployeeIdAndStartDateBetween(1L, from, to)).thenReturn(spells);

        SicknessPatternService.SicknessStatistics stats =
                sicknessPatternService.calculateStatistics(1L, from, to);

        assertEquals(2, stats.numberOfSpells());
        assertEquals(4.0, stats.totalDaysLost());
        assertEquals(2.0, stats.averageSpellLengthDays());
    }

    @Test
    void calculateStatistics_withNoSpells_averagesToZeroRatherThanDividingByZero() {
        LocalDate from = LocalDate.of(2026, 1, 1);
        LocalDate to = LocalDate.of(2026, 3, 31);
        when(absenceRepository.findByEmployeeIdAndStartDateBetween(1L, from, to)).thenReturn(List.of());

        SicknessPatternService.SicknessStatistics stats =
                sicknessPatternService.calculateStatistics(1L, from, to);

        assertEquals(0, stats.numberOfSpells());
        assertEquals(0.0, stats.averageSpellLengthDays());
    }
}
