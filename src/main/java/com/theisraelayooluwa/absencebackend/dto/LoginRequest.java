package com.theisraelayooluwa.absencebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Employee login payload")
public record LoginRequest(
        @Schema(description = "Employee email", example = "ava.smith@company.com")
        @NotBlank String email,
        @Schema(example = "ChangeMe123!")
        @NotBlank String password
) {
}
