package com.theisraelayooluwa.absencebackend.dto;

import com.theisraelayooluwa.absencebackend.model.Absence;
import com.theisraelayooluwa.absencebackend.model.AbsenceType;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Unified payload for recording both planned and unplanned absences")
public record RecordAbsenceDto(
        @Schema(example = "1")
        @NotNull Long employeeId,
        @Schema(example = "HOLIDAY", description = "Absence type (HOLIDAY, OTHER_LEAVE, SCHEDULED_TRAINING for planned; SICKNESS, UNEXPLAINED_ABSENCE for unplanned)")
        @NotNull AbsenceType type,
        @Schema(example = "2026-07-20")
        @NotNull LocalDate startDate,
        @Schema(example = "2026-07-24")
        @NotNull LocalDate endDate,
        @Schema(example = "COMPASSIONATE", description = "Required for OTHER_LEAVE type")
        String otherLeaveCategory,
        @Schema(example = "EMPLOYEE", description = "Required for unplanned absences only")
        Absence.RecordedBy recordedBy,
        @Schema(example = "Summer leave")
        String notes
) {
}
