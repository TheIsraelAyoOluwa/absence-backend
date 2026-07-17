package com.theisraelayooluwa.absencebackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.Year;




@Entity
@Table(name = "holiday_years")
@Getter
@Setter
@NoArgsConstructor


public class HolidayYear {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "engagement_id")
    private Engagement engagement;

    @Schema(example = "2026-04-01")
    @Column(nullable = false)
    private LocalDate startDate;

    @Schema(example = "2027-03-31")
    @Column(nullable = false)
    private LocalDate endDate;

    /** Maximum number of hours that may carry over INTO this holiday year from the previous one. */
    @Schema(example = "40.0")
    @Column(name = "carry_over_limit_hours", nullable = false)
    private double carryOverLimitHours = 5.0 * 8.0;

    /** Hours actually carried over from the previous holiday year (<= carryOverLimitHours). */
    @Schema(example = "0.0")
    @Column(name = "carried_over_hours", nullable = false)
    private double carriedOverHours = 0.0;

    /** Extra (discretionary) holiday granted for this specific holiday year - slide 12. */
    @Schema(example = "0.0")
    @Column(name = "extra_holiday_hours", nullable = false)
    private double extraHolidayHours = 0.0;

    public boolean isLeapYear() {
        return Year.of(startDate.getYear()).isLeap() || Year.of(endDate.getYear()).isLeap();
    }

    public long lengthInDays() {
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    public boolean contains(LocalDate date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }


}
