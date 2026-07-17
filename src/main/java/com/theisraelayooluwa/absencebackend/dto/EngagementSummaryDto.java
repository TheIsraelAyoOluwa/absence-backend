package com.theisraelayooluwa.absencebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Minimal engagement summary")
public record EngagementSummaryDto(
        @Schema(example = "1")
        Long id,
        @Schema(example = "2026-07-10")
        LocalDate startDate,
        @Schema(example = "2027-07-09")
        LocalDate endDate
) {
}
