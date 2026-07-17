package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.model.EmployeeRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employee directory entry")
public record EmployeeDirectoryDto(
        @Schema(example = "1")
        Long id,
        @Schema(example = "Ava")
        String firstName,
        @Schema(example = "Smith")
        String lastName,
        @Schema(example = "ava.smith@company.com")
        String email,
        @Schema(example = "EMP-1001")
        String payrollNumber,
        @Schema(example = "EMPLOYEE")
        EmployeeRole role,
        @Schema(example = "PENDING")
        Employee.ApprovalStatus approvalStatus,
        @Schema(example = "FULL_TIME")
        Employer.WorkingCriteria workingCriteria,
        @Schema(example = "8.0")
        Double dailyHours,
        @Schema(example = "10")
        Long employerId,
        @Schema(example = "Acme Ltd")
        String employerName
) {
    public static EmployeeDirectoryDto from(Employee employee) {
        return new EmployeeDirectoryDto(
                employee.getId(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getEmail(),
                employee.getPayrollNumber(),
                employee.getRole(),
                employee.getApprovalStatus(),
                employee.getWorkingCriteria(),
                employee.getDailyHours(),
                employee.getEmployer() != null ? employee.getEmployer().getId() : null,
                employee.getEmployer() != null ? employee.getEmployer().getName() : null
        );
    }
}
