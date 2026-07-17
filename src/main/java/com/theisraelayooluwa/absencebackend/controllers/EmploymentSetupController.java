package com.theisraelayooluwa.absencebackend.controllers;

import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.dto.EngagementSummaryDto;
import com.theisraelayooluwa.absencebackend.dto.HolidayYearDto;
import com.theisraelayooluwa.absencebackend.dto.TermDto;
import com.theisraelayooluwa.absencebackend.dto.WorkPatternDto;
import com.theisraelayooluwa.absencebackend.model.Engagement;
import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import com.theisraelayooluwa.absencebackend.model.Term;
import com.theisraelayooluwa.absencebackend.model.WorkPattern;
import com.theisraelayooluwa.absencebackend.repository.EngagementRepository;
import com.theisraelayooluwa.absencebackend.repository.HolidayYearRepository;
import com.theisraelayooluwa.absencebackend.repository.TermRepository;
import com.theisraelayooluwa.absencebackend.repository.WorkPatternRepository;
import com.theisraelayooluwa.absencebackend.services.EntityMapperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Employment Setup", description = "Work patterns, terms, and holiday years")
@SecurityRequirement(name = "bearerAuth")
public class EmploymentSetupController {

    private final EngagementRepository engagementRepository;
    private final WorkPatternRepository workPatternRepository;
    private final TermRepository termRepository;
    private final HolidayYearRepository holidayYearRepository;
    private final EntityMapperService entityMapperService;

    public EmploymentSetupController(EngagementRepository engagementRepository,
                                     WorkPatternRepository workPatternRepository,
                                     TermRepository termRepository,
                                     HolidayYearRepository holidayYearRepository,
                                     EntityMapperService entityMapperService) {
        this.engagementRepository = engagementRepository;
        this.workPatternRepository = workPatternRepository;
        this.termRepository = termRepository;
        this.holidayYearRepository = holidayYearRepository;
        this.entityMapperService = entityMapperService;
    }

    @PostMapping("/work-patterns")
    @Operation(summary = "Create a work pattern")
    public ResponseEntity<ApiResponse<WorkPattern>> createWorkPattern(@Valid @RequestBody WorkPatternDto dto) {
        WorkPattern workPattern = new WorkPattern();
        workPattern.setDescription(dto.description());
        workPattern.setDayHours(dto.dayHours());
        WorkPattern saved = workPatternRepository.save(workPattern);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Work pattern created", saved));
    }

    @GetMapping("/work-patterns")
    @Operation(summary = "Get all work patterns")
    public ResponseEntity<ApiResponse<List<WorkPattern>>> getWorkPatterns() {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Work patterns retrieved", workPatternRepository.findAll()));
    }

    @PostMapping("/terms")
    @Operation(summary = "Create a term")
    public ResponseEntity<ApiResponse<Term>> createTerm(@Valid @RequestBody TermDto dto) {
        Engagement engagement = engagementRepository.findById(dto.engagementId())
                .orElseThrow(() -> new IllegalArgumentException("Engagement not found: " + dto.engagementId()));
        WorkPattern workPattern = workPatternRepository.findById(dto.workPatternId())
                .orElseThrow(() -> new IllegalArgumentException("WorkPattern not found: " + dto.workPatternId()));

        Term term = new Term();
        term.setEngagement(engagement);
        term.setWorkPattern(workPattern);
        term.setStartDate(dto.startDate());
        term.setEndDate(dto.endDate());
        entityMapperService.mapTermDto(dto, term);

        Term saved = termRepository.save(term);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Term created", saved));
    }

    @GetMapping("/terms")
    @Operation(summary = "Get all terms")
    public ResponseEntity<ApiResponse<List<Term>>> getTerms() {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Terms retrieved", termRepository.findAll()));
    }

    @GetMapping("/engagements/{engagementId}/terms")
    @Operation(summary = "Get terms for an engagement")
    public ResponseEntity<ApiResponse<List<Term>>> getTermsForEngagement(@PathVariable Long engagementId) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Terms retrieved",
                termRepository.findByEngagementIdOrderByStartDateAsc(engagementId)));
    }

    @GetMapping("/employees/{employeeId}/engagements")
    @Operation(summary = "Get engagements for an employee")
    public ResponseEntity<ApiResponse<List<EngagementSummaryDto>>> getEngagementsForEmployee(@PathVariable Long employeeId) {
        List<EngagementSummaryDto> engagements = engagementRepository.findByEmployeeId(employeeId).stream()
                .map(engagement -> new EngagementSummaryDto(
                        engagement.getId(),
                        engagement.getStartDate(),
                        engagement.getEndDate()
                ))
                .toList();

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Engagements retrieved", engagements));
    }

    @PostMapping("/holiday-years")
    @Operation(summary = "Create a holiday year")
    public ResponseEntity<ApiResponse<HolidayYear>> createHolidayYear(@Valid @RequestBody HolidayYearDto dto) {
        Engagement engagement = engagementRepository.findById(dto.engagementId())
                .orElseThrow(() -> new IllegalArgumentException("Engagement not found: " + dto.engagementId()));

        HolidayYear holidayYear = new HolidayYear();
        holidayYear.setEngagement(engagement);
        holidayYear.setStartDate(dto.startDate());
        holidayYear.setEndDate(dto.endDate());
        entityMapperService.mapHolidayYearDto(dto, holidayYear);

        HolidayYear saved = holidayYearRepository.save(holidayYear);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Holiday year created", saved));
    }

    @GetMapping("/holiday-years")
    @Operation(summary = "Get all holiday years")
    public ResponseEntity<ApiResponse<List<HolidayYear>>> getHolidayYears() {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Holiday years retrieved", holidayYearRepository.findAll()));
    }

    @GetMapping("/engagements/{engagementId}/holiday-years")
    @Operation(summary = "Get holiday years for an engagement")
    public ResponseEntity<ApiResponse<List<HolidayYear>>> getHolidayYearsForEngagement(@PathVariable Long engagementId) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Holiday years retrieved",
                holidayYearRepository.findByEngagementIdOrderByStartDateAsc(engagementId)));
    }
}
