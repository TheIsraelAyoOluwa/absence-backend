package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.EmployeeRole;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Employee login response")
public record LoginResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(example = "2026-07-16T17:15:53Z")
        Instant expiresAt,
        @Schema(example = "1")
        Long employeeId,
        @Schema(example = "ava.smith@company.com")
        String email,
        @Schema(example = "EMP-1001")
        String payrollNumber,
        @Schema(example = "EMPLOYEE")
        EmployeeRole role
) {
}
