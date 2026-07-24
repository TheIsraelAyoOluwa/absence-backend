package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.dto.EmployeeDto;
import com.theisraelayooluwa.absencebackend.dto.EmployeeUpdateDto;
import com.theisraelayooluwa.absencebackend.model.*;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EngagementRepository;
import com.theisraelayooluwa.absencebackend.repository.HolidayYearRepository;
import com.theisraelayooluwa.absencebackend.repository.EmployerRepository;
import com.theisraelayooluwa.absencebackend.repository.TermRepository;
import com.theisraelayooluwa.absencebackend.repository.WorkPatternRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
public class EmployeeManagementService {

    private final EmployeeRepository employeeRepository;
    private final EmployerRepository employerRepository;
    private final EngagementRepository engagementRepository;
    private final WorkPatternRepository workPatternRepository;
    private final TermRepository termRepository;
    private final HolidayYearRepository holidayYearRepository;
    private final LeaveUnitService leaveUnitService;
    private final PasswordEncoder passwordEncoder;

    public EmployeeManagementService(EmployeeRepository employeeRepository,
                                     EmployerRepository employerRepository,
                                     EngagementRepository engagementRepository,
                                     WorkPatternRepository workPatternRepository,
                                     TermRepository termRepository,
                                     HolidayYearRepository holidayYearRepository,
                                     LeaveUnitService leaveUnitService,
                                     PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.employerRepository = employerRepository;
        this.engagementRepository = engagementRepository;
        this.workPatternRepository = workPatternRepository;
        this.termRepository = termRepository;
        this.holidayYearRepository = holidayYearRepository;
        this.leaveUnitService = leaveUnitService;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Employee register(EmployeeDto dto) {
        if (employeeRepository.findByPayrollNumber(dto.payrollNumber()).isPresent()) {
            throw new IllegalStateException("Employee payroll number already exists: " + dto.payrollNumber());
        }
        if (employeeRepository.findByEmail(dto.email()).isPresent()) {
            throw new IllegalStateException("Employee email already exists: " + dto.email());
        }

        Employer employer = employerRepository.findFirstByNameIgnoreCaseOrderByIdAsc(dto.employerName())
                .orElseThrow(() -> new IllegalArgumentException("Employer not found: " + dto.employerName()));

        WorkPattern workPattern = workPatternRepository.findById(dto.workPatternId())
                .orElseThrow(() -> new IllegalArgumentException("Work pattern not found: " + dto.workPatternId()));
        if (workPattern.getEmployer() == null || !workPattern.getEmployer().getId().equals(employer.getId())) {
            throw new IllegalArgumentException("Work pattern does not belong to employer: " + dto.employerName());
        }

        Employee employee = new Employee();
        employee.setFirstName(dto.firstName());
        employee.setLastName(dto.lastName());
        employee.setPayrollNumber(dto.payrollNumber());
        employee.setEmail(dto.email());
        employee.setPasswordHash(passwordEncoder.encode(dto.password()));
        employee.setRole(dto.role());
        employee.setEmployer(employer);
        employee.setWorkPattern(workPattern);
        // New employees start in PENDING state - employers must approve before login is allowed
        employee.setApprovalStatus(Employee.ApprovalStatus.PENDING);
        // Inherit employer's working criteria and daily hours if not specified by employee
        employee.setWorkingCriteria(dto.workingCriteria() != null ? dto.workingCriteria() : employer.getWorkingCriteria());
        employee.setDailyHours(dto.dailyHours() != null ? dto.dailyHours() : employer.getStandardDailyHours());

        return employeeRepository.save(employee);
    }

    public List<Employee> findPendingEmployees(Long employerId) {
        return employeeRepository.findByEmployerIdAndApprovalStatusOrderByLastNameAsc(employerId, Employee.ApprovalStatus.PENDING);
    }

    @Transactional
    public Employee approveEmployee(Long employerId, Long employeeId) {
        Employee employee = getEmployerEmployee(employerId, employeeId);
        if (employee.getApprovalStatus() != Employee.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only pending employees can be approved");
        }

        Employer employer = employee.getEmployer();
        double dailyHours = leaveUnitService.resolveDailyHours(employee);
        employee.setDailyHours(dailyHours);
        if (employee.getWorkingCriteria() == null) {
            employee.setWorkingCriteria(employer.getWorkingCriteria());
        }
        employee.setApprovalStatus(Employee.ApprovalStatus.APPROVED);
        Employee savedEmployee = employeeRepository.save(employee);

        // Create employment records when employee is approved: Engagement, Term, HolidayYear
        // These must exist before employee can request leave
        Engagement engagement = new Engagement();
        engagement.setEmployee(savedEmployee);
        engagement.setEmployer(employer);
        engagement.setStartDate(LocalDate.now());
        Engagement savedEngagement = engagementRepository.save(engagement);

        // Reuse the work pattern the employee selected from the employer's catalog at signup.
        // Legacy employees registered before that field existed fall back to an auto-generated default.
        WorkPattern savedPattern = savedEmployee.getWorkPattern();
        if (savedPattern == null) {
            WorkPattern workPattern = new WorkPattern();
            workPattern.setDescription("Default " + savedEmployee.getWorkingCriteria() + " pattern");
            workPattern.setEmployer(employer);
            Map<java.time.DayOfWeek, Double> dayHours = new EnumMap<>(java.time.DayOfWeek.class);
            dayHours.put(java.time.DayOfWeek.MONDAY, dailyHours);
            dayHours.put(java.time.DayOfWeek.TUESDAY, dailyHours);
            dayHours.put(java.time.DayOfWeek.WEDNESDAY, dailyHours);
            dayHours.put(java.time.DayOfWeek.THURSDAY, dailyHours);
            dayHours.put(java.time.DayOfWeek.FRIDAY, dailyHours);
            workPattern.setDayHours(dayHours);
            savedPattern = workPatternRepository.save(workPattern);
        }

        Term term = new Term();
        term.setEngagement(savedEngagement);
        term.setWorkPattern(savedPattern);
        term.setStartDate(LocalDate.now());
        term.setAnnualEntitlementHours(Employee.STATUTORY_MINIMUM_HOLIDAY_DAYS * dailyHours);
        termRepository.save(term);

        LeaveUnitService.FiscalYearWindow fiscalYearWindow = leaveUnitService.resolveFiscalYearWindow(employer, LocalDate.now());
        HolidayYear holidayYear = new HolidayYear();
        holidayYear.setEngagement(savedEngagement);
        holidayYear.setStartDate(fiscalYearWindow.startDate());
        holidayYear.setEndDate(fiscalYearWindow.endDate());
        holidayYearRepository.save(holidayYear);
        return savedEmployee;
    }

    @Transactional
    public Employee rejectEmployee(Long employerId, Long employeeId) {
        Employee employee = getEmployerEmployee(employerId, employeeId);
        if (employee.getApprovalStatus() != Employee.ApprovalStatus.PENDING) {
            throw new IllegalStateException("Only pending employees can be rejected");
        }

        employee.setApprovalStatus(Employee.ApprovalStatus.REJECTED);
        return employeeRepository.save(employee);
    }

    @Transactional
    public Employee updateEmployee(Long employerId, Long employeeId, EmployeeUpdateDto dto) {
        Employee employee = getEmployerEmployee(employerId, employeeId);

        if (dto.firstName() != null) {
            employee.setFirstName(dto.firstName());
        }
        if (dto.lastName() != null) {
            employee.setLastName(dto.lastName());
        }
        if (dto.payrollNumber() != null) {
            employeeRepository.findByPayrollNumber(dto.payrollNumber())
                    .filter(existing -> !existing.getId().equals(employeeId))
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Employee payroll number already exists: " + dto.payrollNumber());
                    });
            employee.setPayrollNumber(dto.payrollNumber());
        }
        if (dto.email() != null) {
            employeeRepository.findByEmail(dto.email())
                    .filter(existing -> !existing.getId().equals(employeeId))
                    .ifPresent(existing -> {
                        throw new IllegalStateException("Employee email already exists: " + dto.email());
                    });
            employee.setEmail(dto.email());
        }
        if (dto.role() != null) {
            employee.setRole(dto.role());
        }
        if (dto.workingCriteria() != null) {
            employee.setWorkingCriteria(dto.workingCriteria());
        }
        if (dto.dailyHours() != null && dto.dailyHours() > 0) {
            employee.setDailyHours(dto.dailyHours());
        }

        return employeeRepository.save(employee);
    }

    private Employee getEmployerEmployee(Long employerId, Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + employeeId));
        Employer employer = employee.getEmployer();
        if (employer == null || !employer.getId().equals(employerId)) {
            throw new IllegalArgumentException("Employee not found for employer: " + employerId);
        }
        return employee;
    }
}
