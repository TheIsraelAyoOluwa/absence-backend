package com.theisraelayooluwa.absencebackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.DayOfWeek;
import java.util.HashMap;
import java.util.Map;



/**
 * Describes a work pattern, per the briefing (slide 9):
 *  - "A number of days of equal length" (e.g. Mon-Fri, 5 days/week), or
 *  - "Specific hours on specific days" (e.g. Tue=6h, Wed=8h, Thu=8h)
 * <p>
 * Both cases are represented uniformly as a map of DayOfWeek -> hours worked,
 * so downstream entitlement/proration logic doesn't need to branch on which
 * style was used to define the pattern.
 */

@Entity
@Table(name = "work_patterns")
@Getter
@Setter
@NoArgsConstructor


public class WorkPattern {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String description;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "employer_id")
    private Employer employer;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "work_pattern_days", joinColumns = @JoinColumn(name = "work_pattern_id"))
    @MapKeyColumn(name = "day_of_week")
    @MapKeyEnumerated(EnumType.STRING)
    @Column(name = "hours")
    private Map<DayOfWeek, Double> dayHours = new HashMap<>();

    public int getDaysPerWeek() {
        return (int) dayHours.values().stream().filter(h -> h > 0).count();
    }

    public double getTotalWeeklyHours() {
        return dayHours.values().stream().mapToDouble(Double::doubleValue).sum();
    }

    /** Fraction of a standard 5-day full-time pattern this pattern represents (used for pro rata entitlement). */
    public double getFullTimeEquivalentFraction(double fullTimeWeeklyHours) {
        if (fullTimeWeeklyHours <= 0) {
            return 1.0;
        }
        return getTotalWeeklyHours() / fullTimeWeeklyHours;
    }
}
