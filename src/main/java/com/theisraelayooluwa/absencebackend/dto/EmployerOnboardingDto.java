package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.Employer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Employer onboarding payload")
public record EmployerOnboardingDto(
        @Schema(example = "Acme Ltd")
        @NotBlank String name,
        @Schema(example = "hr@acme.com")
        @NotBlank @Email String email,
        @Schema(example = "ChangeMe123!")
        @NotBlank String password,
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
