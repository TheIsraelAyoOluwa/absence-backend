package com.theisraelayooluwa.absencebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

@Schema(description = "Holiday year payload")
public record HolidayYearDto(
        @Schema(example = "1")
        @NotNull Long engagementId,
        @Schema(example = "2026-07-10")
        @NotNull LocalDate startDate,
        @Schema(example = "2027-07-09")
        @NotNull LocalDate endDate,
        @Schema(example = "40.0")
        Double carryOverLimitHours,
        @Schema(example = "0.0")
        Double carriedOverHours,
        @Schema(example = "0.0")
        Double extraHolidayHours
) {
}
