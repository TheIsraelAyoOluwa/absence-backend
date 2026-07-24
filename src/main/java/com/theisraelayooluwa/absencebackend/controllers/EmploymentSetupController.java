package com.theisraelayooluwa.absencebackend.controllers;

import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.dto.EngagementSummaryDto;
import com.theisraelayooluwa.absencebackend.dto.HolidayYearDto;
import com.theisraelayooluwa.absencebackend.dto.TermDto;
import com.theisraelayooluwa.absencebackend.dto.WorkPatternDto;
import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.model.Engagement;
import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import com.theisraelayooluwa.absencebackend.model.Term;
import com.theisraelayooluwa.absencebackend.model.WorkPattern;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EmployerRepository;
import com.theisraelayooluwa.absencebackend.repository.EngagementRepository;
import com.theisraelayooluwa.absencebackend.repository.HolidayYearRepository;
import com.theisraelayooluwa.absencebackend.repository.TermRepository;
import com.theisraelayooluwa.absencebackend.repository.WorkPatternRepository;
import com.theisraelayooluwa.absencebackend.services.EmployeeAccessService;
import com.theisraelayooluwa.absencebackend.services.EntityMapperService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    private final EmployeeAccessService employeeAccessService;
    private final EmployerRepository employerRepository;
    private final EmployeeRepository employeeRepository;

    public EmploymentSetupController(EngagementRepository engagementRepository,
                                     WorkPatternRepository workPatternRepository,
                                     TermRepository termRepository,
                                     HolidayYearRepository holidayYearRepository,
                                     EntityMapperService entityMapperService,
                                     EmployeeAccessService employeeAccessService,
                                     EmployerRepository employerRepository,
                                     EmployeeRepository employeeRepository) {
        this.engagementRepository = engagementRepository;
        this.workPatternRepository = workPatternRepository;
        this.termRepository = termRepository;
        this.holidayYearRepository = holidayYearRepository;
        this.entityMapperService = entityMapperService;
        this.employeeAccessService = employeeAccessService;
        this.employerRepository = employerRepository;
        this.employeeRepository = employeeRepository;
    }

    private Employee currentEmployee(Authentication authentication) {
        return employeeRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + authentication.getName()));
    }

    @PostMapping("/work-patterns")
    @Operation(summary = "Create a work pattern for the signed-in employer")
    public ResponseEntity<ApiResponse<WorkPattern>> createWorkPattern(@Valid @RequestBody WorkPatternDto dto,
                                                                       Authentication authentication) {
        Employer employer = currentEmployer(authentication);
        WorkPattern workPattern = new WorkPattern();
        workPattern.setDescription(dto.description());
        workPattern.setDayHours(dto.dayHours());
        workPattern.setEmployer(employer);
        WorkPattern saved = workPatternRepository.save(workPattern);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Work pattern created", saved));
    }

    @GetMapping("/work-patterns")
    @Operation(summary = "Get work patterns for the signed-in employer")
    public ResponseEntity<ApiResponse<List<WorkPattern>>> getWorkPatterns(Authentication authentication) {
        Employer employer = currentEmployer(authentication);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Work patterns retrieved",
                workPatternRepository.findByEmployerIdOrderByDescriptionAsc(employer.getId())));
    }

    @GetMapping("/employers/{employerId}/work-patterns")
    @Operation(summary = "List work patterns for an employer (public, for the employee signup picker)")
    public ResponseEntity<ApiResponse<List<WorkPattern>>> getWorkPatternsForEmployer(@PathVariable Long employerId) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Work patterns retrieved",
                workPatternRepository.findByEmployerIdOrderByDescriptionAsc(employerId)));
    }

    private Employer currentEmployer(Authentication authentication) {
        return employerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Employer not found: " + authentication.getName()));
    }

    @PostMapping("/terms")
    @Operation(summary = "Create a term")
    public ResponseEntity<ApiResponse<Term>> createTerm(@Valid @RequestBody TermDto dto,
                                                         Authentication authentication) {
        employeeAccessService.ensureCanViewEngagement(authentication.getName(), dto.engagementId());
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
    @Operation(summary = "Get terms visible to the caller: their own, or the whole company for managers/C-level")
    public ResponseEntity<ApiResponse<List<Term>>> getTerms(Authentication authentication) {
        Employee caller = currentEmployee(authentication);
        Long employerId = caller.getEmployer() != null ? caller.getEmployer().getId() : null;
        List<Term> terms = termRepository.findAll().stream()
                .filter(term -> employerId != null && employerId.equals(term.getEngagement().getEmployer().getId()))
                .filter(term -> caller.canApproveLeave() || term.getEngagement().getEmployee().getId().equals(caller.getId()))
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Terms retrieved", terms));
    }

    @GetMapping("/engagements/{engagementId}/terms")
    @Operation(summary = "Get terms for an engagement")
    public ResponseEntity<ApiResponse<List<Term>>> getTermsForEngagement(@PathVariable Long engagementId,
                                                                          Authentication authentication) {
        employeeAccessService.ensureCanViewEngagement(authentication.getName(), engagementId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Terms retrieved",
                termRepository.findByEngagementIdOrderByStartDateAsc(engagementId)));
    }

    @GetMapping("/employees/{employeeId}/engagements")
    @Operation(summary = "Get engagements for an employee")
    public ResponseEntity<ApiResponse<List<EngagementSummaryDto>>> getEngagementsForEmployee(
            @PathVariable Long employeeId, Authentication authentication) {
        employeeAccessService.ensureCanView(authentication.getName(), employeeId);
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
    public ResponseEntity<ApiResponse<HolidayYear>> createHolidayYear(@Valid @RequestBody HolidayYearDto dto,
                                                                       Authentication authentication) {
        employeeAccessService.ensureCanViewEngagement(authentication.getName(), dto.engagementId());
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
    @Operation(summary = "Get holiday years visible to the caller: their own, or the whole company for managers/C-level")
    public ResponseEntity<ApiResponse<List<HolidayYear>>> getHolidayYears(Authentication authentication) {
        Employee caller = currentEmployee(authentication);
        Long employerId = caller.getEmployer() != null ? caller.getEmployer().getId() : null;
        List<HolidayYear> holidayYears = holidayYearRepository.findAll().stream()
                .filter(hy -> employerId != null && employerId.equals(hy.getEngagement().getEmployer().getId()))
                .filter(hy -> caller.canApproveLeave() || hy.getEngagement().getEmployee().getId().equals(caller.getId()))
                .toList();
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Holiday years retrieved", holidayYears));
    }

    @GetMapping("/engagements/{engagementId}/holiday-years")
    @Operation(summary = "Get holiday years for an engagement")
    public ResponseEntity<ApiResponse<List<HolidayYear>>> getHolidayYearsForEngagement(@PathVariable Long engagementId,
                                                                                        Authentication authentication) {
        employeeAccessService.ensureCanViewEngagement(authentication.getName(), engagementId);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Holiday years retrieved",
                holidayYearRepository.findByEngagementIdOrderByStartDateAsc(engagementId)));
    }
}
