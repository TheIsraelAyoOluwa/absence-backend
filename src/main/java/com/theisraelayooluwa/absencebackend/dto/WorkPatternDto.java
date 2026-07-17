package com.theisraelayooluwa.absencebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.time.DayOfWeek;
import java.util.Map;

@Schema(description = "Work pattern payload")
public record WorkPatternDto(
        @Schema(example = "Standard Monday-Friday pattern")
        @NotBlank String description,
        @Schema(description = "Hours worked per day of week", example = "{\"MONDAY\":8.0,\"TUESDAY\":8.0}")
        @NotEmpty Map<DayOfWeek, Double> dayHours
) {
}
