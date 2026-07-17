package com.theisraelayooluwa.absencebackend.controllers;

import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.dto.EmployeeDirectoryDto;
import com.theisraelayooluwa.absencebackend.dto.EmployeeUpdateDto;
import com.theisraelayooluwa.absencebackend.dto.EmployerLoginRequest;
import com.theisraelayooluwa.absencebackend.dto.EmployerLoginResponse;
import com.theisraelayooluwa.absencebackend.dto.EmployerOnboardingDto;
import com.theisraelayooluwa.absencebackend.dto.EmployerDto;
import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EmployerRepository;
import com.theisraelayooluwa.absencebackend.services.EmployeeManagementService;
import com.theisraelayooluwa.absencebackend.services.EntityMapperService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/employers")
@Tag(name = "Employers", description = "Employer onboarding and management endpoints")
public class EmployerController {

    private final EmployerRepository employerRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeManagementService employeeManagementService;
    private final EntityMapperService entityMapperService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final com.theisraelayooluwa.absencebackend.security.TokenService tokenService;

    public EmployerController(EmployerRepository employerRepository,
                              EmployeeRepository employeeRepository,
                              EmployeeManagementService employeeManagementService,
                              EntityMapperService entityMapperService,
                              PasswordEncoder passwordEncoder,
                              AuthenticationManager authenticationManager,
                              com.theisraelayooluwa.absencebackend.security.TokenService tokenService) {
        this.employerRepository = employerRepository;
        this.employeeRepository = employeeRepository;
        this.employeeManagementService = employeeManagementService;
        this.entityMapperService = entityMapperService;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/onboard")
    @Operation(summary = "Onboard a new employer")
    public ResponseEntity<ApiResponse<Employer>> onboard(@Valid @RequestBody EmployerOnboardingDto dto) {
        if (employerRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalStateException("Employer email already exists: " + dto.email());
        }

        // Create employer with custom working criteria and fiscal year configuration
        Employer employer = new Employer();
        employer.setEmail(dto.email());
        employer.setPasswordHash(passwordEncoder.encode(dto.password()));
        entityMapperService.mapEmployerOnboardingDto(dto, employer);

        Employer saved = employerRepository.save(employer);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Employer onboarded", saved));
    }

    @PostMapping("/login")
    @Operation(summary = "Login employer")
    public ResponseEntity<ApiResponse<EmployerLoginResponse>> login(@Valid @RequestBody EmployerLoginRequest dto) {
        // Authenticate against both Employee and Employer tables (EmployeeUserDetailsService handles both)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        Employer employer = employerRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalArgumentException("Employer not found: " + dto.email()));

        // Issue JWT token with employer email as subject
        String token = tokenService.createToken(employer.getEmail());
        Instant expiresAt = tokenService.expirationInstant();
        EmployerLoginResponse response = new EmployerLoginResponse(
                token,
                "Bearer",
                expiresAt,
                employer.getId(),
                employer.getEmail(),
                employer.getName()
        );

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employer login successful", response));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the signed-in employer profile")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Employer>> me(Authentication authentication) {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employer retrieved", currentEmployer(authentication)));
    }

    @PutMapping("/me")
    @Operation(summary = "Update the signed-in employer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Employer>> updateMe(Authentication authentication,
                                                          @Valid @RequestBody EmployerDto dto) {
        Employer employer = currentEmployer(authentication);
        entityMapperService.mapEmployerDto(dto, employer);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employer updated", employerRepository.save(employer)));
    }

    @GetMapping("/me/employees")
    @Operation(summary = "Get employees for the signed-in employer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<EmployeeDirectoryDto>>> getEmployees(Authentication authentication) {
        Employer employer = currentEmployer(authentication);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employees retrieved",
                entityMapperService.toEmployeeDirectoryDtos(employeeRepository.findByEmployerIdOrderByLastNameAsc(employer.getId()))));
    }

    @GetMapping("/employees")
    @Operation(summary = "Get all employees across all employers")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<EmployeeDirectoryDto>>> getAllEmployees(Authentication authentication) {
        currentEmployer(authentication);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employees retrieved",
                entityMapperService.toEmployeeDirectoryDtos(employeeRepository.findAll())));
    }

    @GetMapping("/me/employees/pending")
    @Operation(summary = "Get pending employee sign-ups for the signed-in employer")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<EmployeeDirectoryDto>>> getPendingEmployees(Authentication authentication) {
        Employer employer = currentEmployer(authentication);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Pending employees retrieved",
                entityMapperService.toEmployeeDirectoryDtos(employeeManagementService.findPendingEmployees(employer.getId()))));
    }

    @PostMapping("/me/employees/{employeeId}/decision")
    @Operation(summary = "Approve or reject a pending employee")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<EmployeeDirectoryDto>> decideEmployee(Authentication authentication,
                                                                            @PathVariable Long employeeId,
                                                                            @Parameter(description = "true approves, false rejects", example = "true")
                                                                            @RequestParam boolean approve) {
        Employer employer = currentEmployer(authentication);
        // Single endpoint with approve flag: cleaner than separate approve/reject endpoints
        Employee employee = approve
                ? employeeManagementService.approveEmployee(employer.getId(), employeeId)
                : employeeManagementService.rejectEmployee(employer.getId(), employeeId);
        String message = approve ? "Employee approved" : "Employee rejected";
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), message, EmployeeDirectoryDto.from(employee)));
    }

    @PutMapping("/me/employees/{employeeId}")
    @Operation(summary = "Edit an employee's details")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<EmployeeDirectoryDto>> updateEmployee(Authentication authentication,
                                                                            @PathVariable Long employeeId,
                                                                            @RequestBody EmployeeUpdateDto dto) {
        Employer employer = currentEmployer(authentication);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employee updated",
                EmployeeDirectoryDto.from(employeeManagementService.updateEmployee(employer.getId(), employeeId, dto))));
    }

    // Extract authenticated employer from JWT token
    private Employer currentEmployer(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new IllegalArgumentException("Authenticated employer required");
        }
        return employerRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Employer not found: " + authentication.getName()));
    }
}
