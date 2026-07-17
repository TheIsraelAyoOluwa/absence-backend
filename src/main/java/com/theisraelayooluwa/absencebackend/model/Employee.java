package com.theisraelayooluwa.absencebackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    public static final int STATUTORY_MINIMUM_HOLIDAY_DAYS = 28;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Schema(example = "Ava")
    @Column(nullable = false)
    private String firstName;

    @Schema(example = "Smith")
    @Column(nullable = false)
    private String lastName;

    @Schema(example = "EMP-1001")
    @Column(unique = true, nullable = false)
    private String payrollNumber;

    @Schema(example = "ava.smith@company.com")
    @Column(unique = true, nullable = false)
    private String email;

    @Schema(description = "Employer this employee belongs to", accessMode = Schema.AccessMode.READ_ONLY)
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "employer_id")
    private Employer employer;

    @Schema(example = "PENDING")
    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status")
    private ApprovalStatus approvalStatus = ApprovalStatus.PENDING;

    @Schema(example = "FULL_TIME")
    @Enumerated(EnumType.STRING)
    @Column(name = "working_criteria")
    private Employer.WorkingCriteria workingCriteria = Employer.WorkingCriteria.FULL_TIME;

    @Schema(example = "8.0")
    @Column(name = "daily_hours")
    private Double dailyHours;

    @Schema(description = "Hashed employee password", accessMode = Schema.AccessMode.WRITE_ONLY)
    @JsonIgnore
    @Column
    private String passwordHash;

    @Schema(example = "EMPLOYEE")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmployeeRole role = EmployeeRole.EMPLOYEE;

    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Engagement> engagements = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Absence> absences = new ArrayList<>();

    public String getFullName() {
        return firstName + " " + lastName;
    }

    @JsonIgnore
    public boolean canApproveLeave() {
        return role == EmployeeRole.C_LEVEL_EXECUTIVE || role == EmployeeRole.MANAGER;
    }

    public enum ApprovalStatus {
        PENDING, APPROVED, REJECTED
    }
}
