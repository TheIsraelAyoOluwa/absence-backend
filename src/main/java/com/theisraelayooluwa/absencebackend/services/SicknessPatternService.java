package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.model.Absence;
import com.theisraelayooluwa.absencebackend.model.AbsenceType;
import com.theisraelayooluwa.absencebackend.repository.AbsenceRepository;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;


@Service
public class SicknessPatternService {


    private final AbsenceRepository absenceRepository;

    public SicknessPatternService(AbsenceRepository absenceRepository) {
        this.absenceRepository = absenceRepository;
    }

    /**
     * Bradford Factor = S^2 x D, where S = number of separate sickness
     * episodes (spells) and D = total sickness days, both within the
     * given period. A widely used HR metric: frequent short absences
     * score disproportionately higher than one long spell of equal length.
     */
    public BradfordFactorResult calculateBradfordFactor(Long employeeId, LocalDate from, LocalDate to) {
        List<Absence> sicknessSpells = absenceRepository
                .findByEmployeeIdAndStartDateBetween(employeeId, from, to)
                .stream()
                .filter(a -> a.getType() == AbsenceType.SICKNESS)
                .toList();

        int episodes = sicknessSpells.size();
        double totalHours = sicknessSpells.stream()
                .mapToDouble(a -> a.getDurationHours() != null ? a.getDurationHours() : 0.0)
                .sum();
        double totalDays = totalHours / 8.0;

        long bradfordScore = (long) episodes * episodes * Math.round(totalDays);

        return new BradfordFactorResult(employeeId, episodes, totalDays, bradfordScore, from, to);
    }

    /**
     * Counts sickness/unexplained absence occurrences by day of the week, to
     * surface patterns such as habitual Monday or Friday absence.
     */
    public Map<DayOfWeek, Long> absencesByDayOfWeek(Long employeeId) {
        Map<DayOfWeek, Long> counts = new EnumMap<>(DayOfWeek.class);
        for (DayOfWeek d : DayOfWeek.values()) {
            counts.put(d, 0L);
        }

        List<Absence> unplanned = absenceRepository.findByEmployeeId(employeeId).stream()
                .filter(a -> !a.getType().isPlanned())
                .toList();

        for (Absence absence : unplanned) {
            LocalDate d = absence.getStartDate();
            while (!d.isAfter(absence.getEndDate())) {
                counts.merge(d.getDayOfWeek(), 1L, Long::sum);
                d = d.plusDays(1);
            }
        }
        return counts;
    }

    /** Simple sickness statistics: number of spells and total days lost, for management reporting. */
    public SicknessStatistics calculateStatistics(Long employeeId, LocalDate from, LocalDate to) {
        List<Absence> spells = absenceRepository
                .findByEmployeeIdAndStartDateBetween(employeeId, from, to)
                .stream()
                .filter(a -> a.getType() == AbsenceType.SICKNESS)
                .toList();

        double totalHours = spells.stream()
                .mapToDouble(a -> a.getDurationHours() != null ? a.getDurationHours() : 0.0)
                .sum();
        double totalDays = totalHours / 8.0;

        double averageSpellLength = spells.isEmpty() ? 0.0 : totalDays / spells.size();

        return new SicknessStatistics(employeeId, spells.size(), totalDays, averageSpellLength, from, to);
    }

    public record BradfordFactorResult(
            Long employeeId,
            int episodes,
            double totalDays,
            long bradfordScore,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {}

    public record SicknessStatistics(
            Long employeeId,
            int numberOfSpells,
            double totalDaysLost,
            double averageSpellLengthDays,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {}



}
