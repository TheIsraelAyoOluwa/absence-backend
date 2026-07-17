package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.Employer;
import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Employer registration payload")
public record EmployerDto(
        @Schema(example = "Acme Ltd")
        @NotBlank String name,
        @Schema(example = "FULL_TIME")
        Employer.WorkingCriteria workingCriteria,
        @Schema(example = "8.0")
        Double standardDailyHours,
        @Schema(example = "1")
        Integer fiscalYearStartMonth,
        @Schema(example = "1")
        Integer fiscalYearStartDay,
        @Schema(example = "12")
        Integer fiscalYearEndMonth,
        @Schema(example = "31")
        Integer fiscalYearEndDay,
        @Schema(example = "ENGLAND_AND_WALES")
        Employer.PublicHolidayRegion publicHolidayRegion,
        @Schema(example = "true")
        Boolean publicHolidaysIncludedInEntitlement
) {
}
