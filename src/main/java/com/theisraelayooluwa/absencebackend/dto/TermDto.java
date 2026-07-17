package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.RoundingPolicy;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Term payload")
public record TermDto(
        @Schema(example = "1")
        @NotNull Long engagementId,
        @Schema(example = "2")
        @NotNull Long workPatternId,
        @Schema(example = "2026-07-10")
        @NotNull LocalDate startDate,
        @Schema(example = "2027-07-09")
        LocalDate endDate,
        @Schema(example = "224.0")
        Double annualEntitlementHours,
        @Schema(example = "ROUND_NEAREST_HALF_DAY")
        RoundingPolicy roundingPolicy,
        @Schema(example = "0.0")
        Double carriedOverHours
) {
}
