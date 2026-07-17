package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.EmployeeRole;
import com.theisraelayooluwa.absencebackend.model.Employer;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee edit payload")
public record EmployeeUpdateDto(
        @Schema(example = "Ava")
        String firstName,
        @Schema(example = "Smith")
        String lastName,
        @Schema(example = "EMP-1001")
        String payrollNumber,
        @Schema(example = "ava.smith@company.com")
        String email,
        @Schema(example = "C_LEVEL_EXECUTIVE")
        EmployeeRole role,
        @Schema(example = "FULL_TIME")
        Employer.WorkingCriteria workingCriteria,
        @Schema(example = "8.0")
        Double dailyHours
) {
}
