package com.theisraelayooluwa.absencebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Employer login response")
public record EmployerLoginResponse(
        @Schema(example = "eyJhbGciOiJIUzI1NiJ9...")
        String token,
        @Schema(example = "Bearer")
        String tokenType,
        @Schema(example = "2026-07-16T17:15:53Z")
        Instant expiresAt,
        @Schema(example = "1")
        Long employerId,
        @Schema(example = "hr@acme.com")
        String email,
        @Schema(example = "Acme Ltd")
        String name
) {
}
