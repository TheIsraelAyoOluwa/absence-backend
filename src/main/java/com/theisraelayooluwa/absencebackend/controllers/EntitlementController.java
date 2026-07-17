package com.theisraelayooluwa.absencebackend.controllers;

import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import com.theisraelayooluwa.absencebackend.model.Term;
import com.theisraelayooluwa.absencebackend.repository.HolidayYearRepository;
import com.theisraelayooluwa.absencebackend.repository.TermRepository;
import com.theisraelayooluwa.absencebackend.services.EntitlementService;
import com.theisraelayooluwa.absencebackend.services.LeaveUnitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Map;


@RestController
@RequestMapping("/api/entitlement")
@Tag(name = "Entitlement", description = "Holiday entitlement calculations")
@SecurityRequirement(name = "bearerAuth")
public class EntitlementController {

    private final EntitlementService entitlementService;
    private final TermRepository termRepository;
    private final HolidayYearRepository holidayYearRepository;
    private final LeaveUnitService leaveUnitService;

    public EntitlementController(EntitlementService entitlementService,
                                 TermRepository termRepository,
                                 HolidayYearRepository holidayYearRepository,
                                 LeaveUnitService leaveUnitService) {
        this.entitlementService = entitlementService;
        this.termRepository = termRepository;
        this.holidayYearRepository = holidayYearRepository;
        this.leaveUnitService = leaveUnitService;
    }

    @GetMapping("/term/{termId}/holiday-year/{holidayYearId}")
    @Operation(summary = "Calculate entitlement for a term and holiday year")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> calculate(
            @Parameter(description = "Term id", example = "1") @PathVariable Long termId,
            @Parameter(description = "Holiday year id", example = "2") @PathVariable Long holidayYearId) {
        Term term = termRepository.findById(termId)
                .orElseThrow(() -> new IllegalArgumentException("Term not found: " + termId));
        HolidayYear holidayYear = holidayYearRepository.findById(holidayYearId)
                .orElseThrow(() -> new IllegalArgumentException("HolidayYear not found: " + holidayYearId));

        BigDecimal prorated = entitlementService.calculateProratedEntitlement(term, holidayYear);
        BigDecimal totalAvailable = entitlementService.calculateTotalAvailableHours(term, holidayYear);
        BigDecimal forfeited = entitlementService.calculateForfeitedEntitlement(
                BigDecimal.valueOf(term.getAnnualEntitlementHours()), prorated);
        double dailyHours = leaveUnitService.resolveDailyHours(term.getEngagement().getEmployee());

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Entitlement calculated", Map.of(
                "proratedEntitlementHours", prorated,
                "proratedEntitlementDays", leaveUnitService.hoursToDays(prorated, dailyHours),
                "totalAvailableHours", totalAvailable,
                "totalAvailableDays", leaveUnitService.hoursToDays(totalAvailable, dailyHours),
                "forfeitedHours", forfeited,
                "forfeitedDays", leaveUnitService.hoursToDays(forfeited, dailyHours)
        )));
    }
}
