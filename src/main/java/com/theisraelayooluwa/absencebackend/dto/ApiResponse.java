package com.theisraelayooluwa.absencebackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Standard API response wrapper")
public record ApiResponse<T>(
        @Schema(description = "HTTP status code", example = "200")
        int status,
        @Schema(description = "Human-readable message", example = "Employees retrieved")
        String message,
        @Schema(description = "Response payload")
        T data
) {
}
