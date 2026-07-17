package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.EmployeeRole;
import com.theisraelayooluwa.absencebackend.model.Employer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee registration payload")
public record EmployeeDto(
        @Schema(example = "Ava")
        @NotBlank String firstName,
        @Schema(example = "Smith")
        @NotBlank String lastName,
        @Schema(example = "EMP-1001")
        @NotBlank String payrollNumber,
        @Schema(example = "ava.smith@company.com")
        @NotBlank String email,
        @Schema(example = "ChangeMe123!")
        @NotBlank String password,
        @Schema(example = "C_LEVEL_EXECUTIVE")
        @NotNull EmployeeRole role,
        @Schema(example = "FULL_TIME")
        Employer.WorkingCriteria workingCriteria,
        @Schema(example = "8.0")
        Double dailyHours,
        @Schema(description = "Employer name to link the employee to", example = "Acme Corp")
        @NotBlank String employerName
) {
}
