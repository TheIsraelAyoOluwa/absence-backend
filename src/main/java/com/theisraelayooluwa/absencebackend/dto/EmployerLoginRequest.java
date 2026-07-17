package com.theisraelayooluwa.absencebackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employer login payload")
public record EmployerLoginRequest(
        @Schema(example = "hr@acme.com")
        @NotBlank @Email String email,
        @Schema(example = "ChangeMe123!")
        @NotBlank String password
) {
}
