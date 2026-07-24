package com.theisraelayooluwa.absencebackend.controllers;
import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.dto.AbsenceDecisionDto;
import com.theisraelayooluwa.absencebackend.dto.RecordAbsenceDto;
import com.theisraelayooluwa.absencebackend.model.Absence;
import com.theisraelayooluwa.absencebackend.services.AbsenceService;
import com.theisraelayooluwa.absencebackend.services.EmployeeAccessService;
import com.theisraelayooluwa.absencebackend.services.LeaveUnitService;
import com.theisraelayooluwa.absencebackend.services.SicknessPatternService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/api/absences")
@Tag(name = "Absences", description = "Planned and unplanned absence management")
@SecurityRequirement(name = "bearerAuth")
public class AbsenceController {

    private final AbsenceService absenceService;
    private final SicknessPatternService sicknessPatternService;
    private final LeaveUnitService leaveUnitService;
    private final EmployeeAccessService employeeAccessService;

    public AbsenceController(AbsenceService absenceService,
                             SicknessPatternService sicknessPatternService,
                             LeaveUnitService leaveUnitService,
                             EmployeeAccessService employeeAccessService) {
        this.absenceService = absenceService;
        this.sicknessPatternService = sicknessPatternService;
        this.leaveUnitService = leaveUnitService;
        this.employeeAccessService = employeeAccessService;
    }


    // Absence recording: handles both planned (holiday, personal leave) and unplanned (sickness) absences
    @PostMapping("/record")
    @Operation(summary = "Record an absence (planned or unplanned)")
    public ResponseEntity<ApiResponse<Absence>> recordAbsence(@Valid @RequestBody RecordAbsenceDto dto) {
       Absence absence = absenceService.recordAbsence(
                dto.employeeId(), dto.type(), dto.startDate(), dto.endDate(),
               dto.otherLeaveCategory(), dto.recordedBy(), dto.notes());
        return ResponseEntity.status(HttpStatus.CREATED)
               .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Absence recorded", absence));
    }

    @PostMapping("/{id}/decision")
    @Operation(summary = "Handle approval or rejection of a leave request")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Absence>> processLeaveDecision(
            @PathVariable Long id,
            @Valid @RequestBody AbsenceDecisionDto decision,
            Authentication authentication) {
        Absence absence = absenceService.processLeaveDecision(id, decision.decision(), authentication.getName());
        String message = "APPROVE".equalsIgnoreCase(decision.decision()) ? "Absence approved" : "Absence rejected";
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), message, absence));
    }

    // Absence analytics: duration conversion, sickness patterns, Bradford factor
    @GetMapping("/employee/{employeeId}")
    @Operation(summary = "Get all absences for an employee")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<Absence>>> getForEmployee(@PathVariable Long employeeId, Authentication authentication) {
        employeeAccessService.ensureCanView(authentication.getName(), employeeId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Absences retrieved", absenceService.getAbsencesForEmployee(employeeId)));
    }

    @GetMapping("/{absenceId}/duration")
    @Operation(summary = "Get an absence duration in hours and days")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getDuration(@PathVariable Long absenceId) {
        // Convert internal hours storage to display format (hours + days)
        // Uses employee's dailyHours setting, falls back to employer standard, then 8.0
        Absence absence = absenceService.getAbsenceById(absenceId);
        double dailyHours = leaveUnitService.resolveDailyHours(absence.getEmployee());
        BigDecimal hours = BigDecimal.valueOf(absence.getDurationHours() != null ? absence.getDurationHours() : 0.0);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Absence duration retrieved", Map.of(
                "hours", hours,
                "days", leaveUnitService.hoursToDays(hours, dailyHours)
        )));
    }

    @GetMapping("/employee/{employeeId}/bradford-factor")
    @Operation(summary = "Get Bradford factor for an employee")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<SicknessPatternService.BradfordFactorResult>> bradfordFactor(
            @Parameter(description = "Employee id", example = "1") @PathVariable Long employeeId,
            @Parameter(description = "Start date", example = "2026-01-01") @RequestParam LocalDate from,
            @Parameter(description = "End date", example = "2026-12-31") @RequestParam LocalDate to,
            Authentication authentication) {
        employeeAccessService.ensureCanView(authentication.getName(), employeeId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Bradford factor calculated",
                sicknessPatternService.calculateBradfordFactor(employeeId, from, to)));
    }

    @GetMapping("/employee/{employeeId}/statistics")
    @Operation(summary = "Get sickness statistics for an employee")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<SicknessPatternService.SicknessStatistics>> statistics(
            @Parameter(description = "Employee id", example = "1") @PathVariable Long employeeId,
            @Parameter(description = "Start date", example = "2026-01-01") @RequestParam LocalDate from,
            @Parameter(description = "End date", example = "2026-12-31") @RequestParam LocalDate to,
            Authentication authentication) {
        employeeAccessService.ensureCanView(authentication.getName(), employeeId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Sickness statistics calculated",
                sicknessPatternService.calculateStatistics(employeeId, from, to)));
    }

    @GetMapping("/employee/{employeeId}/pattern-by-day")
    @Operation(summary = "Get absence pattern by day of week")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<DayOfWeek, Long>>> patternByDay(@PathVariable Long employeeId, Authentication authentication) {
        employeeAccessService.ensureCanView(authentication.getName(), employeeId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Absence pattern retrieved",
                sicknessPatternService.absencesByDayOfWeek(employeeId)));
    }




}
