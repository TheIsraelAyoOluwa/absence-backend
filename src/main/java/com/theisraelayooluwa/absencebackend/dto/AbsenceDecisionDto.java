package com.theisraelayooluwa.absencebackend.dto;

import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Unified request for approving or rejecting a leave request")
public record AbsenceDecisionDto(
        @Schema(example = "APPROVE", description = "Decision: APPROVE or REJECT")
        @NotNull String decision,
        @Schema(example = "Approved as per policy", description = "Optional reason for the decision")
        String reason
) {
}
