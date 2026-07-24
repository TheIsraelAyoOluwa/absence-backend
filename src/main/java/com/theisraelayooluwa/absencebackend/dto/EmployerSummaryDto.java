package com.theisraelayooluwa.absencebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Public employer summary used to populate the employer picker during employee signup")
public record EmployerSummaryDto(
        @Schema(example = "1") Long id,
        @Schema(example = "Acme Corp") String name
) {
}
