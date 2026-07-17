package com.theisraelayooluwa.absencebackend.model;

import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;



@Entity
@Table(name = "absences")
@Getter
@Setter
@NoArgsConstructor

public class Absence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(description = "Employee this absence belongs to")
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @Schema(example = "SICKNESS")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceType type;

    @Schema(example = "REQUESTED")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AbsenceStatus status;

    @Schema(example = "2026-07-20")
    @Column(nullable = false)
    private LocalDate startDate;

    @Schema(example = "2026-07-21")
    @Column(nullable = false)
    private LocalDate endDate;

    /** Total working hours this absence consumes, derived from the employee's work pattern. */
    @Schema(example = "8.0")
    @Column(name = "duration_hours")
    private Double durationHours;

    /** Who entered the record: EMPLOYEE or EMPLOYER (manager). */
    @Schema(example = "EMPLOYEE")
    @Enumerated(EnumType.STRING)
    @Column(name = "recorded_by", nullable = false)
    private RecordedBy recordedBy;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt = LocalDateTime.now();

    @Column(length = 1000)
    @Schema(example = "Flu")
    private String notes;

    /** Free-text reason category for OTHER_LEAVE, e.g. PARENTAL, COMPASSIONATE, STUDY, ARMED_FORCES, PUBLIC_SERVICE. */
    @Schema(example = "COMPASSIONATE")
    private String otherLeaveCategory;

    public enum RecordedBy {
        EMPLOYEE, EMPLOYER
    }


    @PrePersist
    @PreUpdate
    private void validate() {
        if (endDate.isBefore(startDate)) {
            throw new IllegalStateException("Absence endDate cannot be before startDate");
        }
        // Business rule from slide 5: planned absence is usually entered before the date,
        // whereas unplanned absence is usually entered on/after the date by the employee.
        if (type.isPlanned() && recordedBy == RecordedBy.EMPLOYEE && recordedAt.toLocalDate().isAfter(startDate)) {
            // Not a hard validation error - the briefing notes "can be exceptions" -
            // left as a warning-level business rule enforced in the service layer instead.
            return;
        }
    }

}
