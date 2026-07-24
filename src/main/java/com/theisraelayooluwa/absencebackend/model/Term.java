package com.theisraelayooluwa.absencebackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;




/**
 * "Within an Engagement, Employee can be engaged under different terms.
 * The terms of Employment specify the work pattern and the holiday
 * entitlement within a holiday year." (slide 8)
 * <p>
 * Multiple Terms can exist within one Engagement (e.g. a change of hours),
 * and unused holiday usually carries over freely from one Term to the next
 * within the same Engagement (slide 11).
 */



@Entity
@Table(name = "terms")
@Getter
@Setter
@NoArgsConstructor


public class Term {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "engagement_id")
    private Engagement engagement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "work_pattern_id")
    private WorkPattern workPattern;

    @Schema(example = "2026-04-01")
    @Column(nullable = false)
    private LocalDate startDate;

    /** Null if this is the current, still-active Term. */
    @Schema(example = "2027-03-31")
    private LocalDate endDate;

    /** Full annual holiday entitlement (in hours) granted under this Term, before proration. */
    @Schema(example = "224.0")
    @Column(name = "annual_entitlement_hours", nullable = false)
    private double annualEntitlementHours = Employee.STATUTORY_MINIMUM_HOLIDAY_DAYS * 8.0;

    @Schema(example = "ROUND_NEAREST_HALF_HOUR")
    @Enumerated(EnumType.STRING)
    @Column(name = "rounding_policy", nullable = false)
    private RoundingPolicy roundingPolicy = RoundingPolicy.ROUND_NEAREST_HALF_HOUR;

    /** Hours of unused holiday carried over from the previous Term (usually unlimited). */
    @Schema(example = "0.0")
    @Column(name = "carried_over_hours", nullable = false)
    private double carriedOverHours = 0.0;
}
