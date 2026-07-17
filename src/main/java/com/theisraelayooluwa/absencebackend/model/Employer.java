package com.theisraelayooluwa.absencebackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "employers")
@Getter
@Setter
@NoArgsConstructor


public class Employer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "Acme Ltd")
    @Column(nullable = false)
    private String name;

    @Schema(example = "hr@acme.com")
    @Column(nullable = false, unique = true)
    private String email;

    @Schema(description = "Hashed employer password", accessMode = Schema.AccessMode.WRITE_ONLY)
    @JsonIgnore
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Schema(example = "FULL_TIME")
    @Enumerated(EnumType.STRING)
    @Column(name = "working_criteria")
    private WorkingCriteria workingCriteria = WorkingCriteria.FULL_TIME;

    @Schema(example = "8.0")
    @Column(name = "standard_daily_hours")
    private Double standardDailyHours = 8.0;

    @Schema(example = "1")
    @Column(name = "fiscal_year_start_month")
    private Integer fiscalYearStartMonth = 1;

    @Schema(example = "1")
    @Column(name = "fiscal_year_start_day")
    private Integer fiscalYearStartDay = 1;

    @Schema(example = "12")
    @Column(name = "fiscal_year_end_month")
    private Integer fiscalYearEndMonth = 12;

    @Schema(example = "31")
    @Column(name = "fiscal_year_end_day")
    private Integer fiscalYearEndDay = 31;

    /** England & Wales / Scotland / Northern Ireland - governs the Public Holiday calendar. */
    @Schema(example = "ENGLAND_AND_WALES")
    @Enumerated(EnumType.STRING)
    @Column(name = "public_holiday_region", nullable = false)
    private PublicHolidayRegion publicHolidayRegion = PublicHolidayRegion.ENGLAND_AND_WALES;

    /** Whether Public Holidays count towards paid holiday entitlement or are given as extra. */
    @Schema(example = "true")
    @Column(name = "public_holidays_included_in_entitlement", nullable = false)
    private boolean publicHolidaysIncludedInEntitlement = true;

    @JsonIgnore
    @OneToMany(mappedBy = "employer")
    private List<Engagement> engagements = new ArrayList<>();

    public enum WorkingCriteria {
        FULL_TIME, PART_TIME
    }

    public enum PublicHolidayRegion {
        ENGLAND_AND_WALES, SCOTLAND, NORTHERN_IRELAND
    }
}
