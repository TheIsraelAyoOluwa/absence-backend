package com.theisraelayooluwa.absencebackend.controllers;

import com.theisraelayooluwa.absencebackend.dto.ApiResponse;
import com.theisraelayooluwa.absencebackend.dto.EmployeeDirectoryDto;
import com.theisraelayooluwa.absencebackend.dto.EmployeeDto;
import com.theisraelayooluwa.absencebackend.dto.LoginRequest;
import com.theisraelayooluwa.absencebackend.dto.LoginResponse;
import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.services.EmployeeManagementService;
import com.theisraelayooluwa.absencebackend.services.EntityMapperService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "Employee registration and lookup endpoints")
public class EmployeeController {

    private final EmployeeRepository employeeRepository;
    private final AuthenticationManager authenticationManager;
    private final com.theisraelayooluwa.absencebackend.security.TokenService tokenService;
    private final EmployeeManagementService employeeManagementService;
    private final EntityMapperService entityMapperService;

    public EmployeeController(EmployeeRepository employeeRepository,
                              AuthenticationManager authenticationManager,
                              com.theisraelayooluwa.absencebackend.security.TokenService tokenService,
                              EmployeeManagementService employeeManagementService,
                              EntityMapperService entityMapperService) {
        this.employeeRepository = employeeRepository;
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
        this.employeeManagementService = employeeManagementService;
        this.entityMapperService = entityMapperService;
    }

    @GetMapping
    @Operation(summary = "Get all employees")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<List<EmployeeDirectoryDto>>> getAll() {
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employees retrieved",
                entityMapperService.toEmployeeDirectoryDtos(employeeRepository.findAll())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get employee by id")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<EmployeeDirectoryDto>> getById(@PathVariable Long id) {
        return employeeRepository.findById(id)
                .map(employee -> ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employee retrieved", EmployeeDirectoryDto.from(employee))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse<>(HttpStatus.NOT_FOUND.value(), "Employee not found: " + id, null)));
    }

    @PostMapping
    @Operation(summary = "Register employee")
    public ResponseEntity<ApiResponse<EmployeeDirectoryDto>> create(@Valid @RequestBody EmployeeDto dto) {
        Employee saved = employeeManagementService.register(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(HttpStatus.CREATED.value(), "Employee signup submitted for approval", EmployeeDirectoryDto.from(saved)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login employee")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest dto) {
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.email(), dto.password()));

        Employee employee = employeeRepository.findByEmail(dto.email())
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + dto.email()));
        String token = tokenService.createToken(authentication.getName());
        Instant expiresAt = tokenService.expirationInstant();

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                expiresAt,
                employee.getId(),
                employee.getEmail(),
                employee.getPayrollNumber(),
                employee.getRole()
        );

        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Login successful", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete employee")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        employeeRepository.deleteById(id);
        return ResponseEntity.ok(new ApiResponse<>(HttpStatus.OK.value(), "Employee deleted", null));
    }
}
