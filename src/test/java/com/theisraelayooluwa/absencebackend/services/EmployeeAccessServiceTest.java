package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.exception.ForbiddenOperationException;
import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.EmployeeRole;
import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.model.Engagement;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EngagementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * This is the service that closes the gap a coarse, path-based Spring Security
 * role match cannot: "read my own record" vs "read a colleague's record" only
 * differ once a specific target id is known, so the rule has to live here
 * rather than in a {@code SecurityConfig} matcher.
 */
@ExtendWith(MockitoExtension.class)
class EmployeeAccessServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EngagementRepository engagementRepository;

    private EmployeeAccessService accessService;

    private Employer employerA;
    private Employer employerB;

    @BeforeEach
    void setUp() {
        accessService = new EmployeeAccessService(employeeRepository, engagementRepository);
        employerA = employerWithId(1L);
        employerB = employerWithId(2L);
    }

    private static Employer employerWithId(long id) {
        Employer employer = new Employer();
        employer.setId(id);
        return employer;
    }

    private static Employee employee(long id, EmployeeRole role, Employer employer) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setRole(role);
        employee.setEmployer(employer);
        return employee;
    }

    @Test
    void ensureCanView_allowsAnyoneToViewTheirOwnRecord() {
        Employee self = employee(1L, EmployeeRole.EMPLOYEE, employerA);
        when(employeeRepository.findByEmail("self@company.com")).thenReturn(Optional.of(self));

        assertDoesNotThrow(() -> accessService.ensureCanView("self@company.com", 1L));
        // Self-access must short-circuit before any lookup of the (non-existent) target record.
        verify(employeeRepository, never()).findById(anyLong());
    }

    @Test
    void ensureCanView_plainEmployeeCannotViewAnotherEmployeesRecord() {
        Employee caller = employee(1L, EmployeeRole.EMPLOYEE, employerA);
        when(employeeRepository.findByEmail("caller@company.com")).thenReturn(Optional.of(caller));

        ForbiddenOperationException exception = assertThrows(ForbiddenOperationException.class,
                () -> accessService.ensureCanView("caller@company.com", 2L));
        assertTrue(exception.getMessage().contains("managers or C-level"));
    }

    @Test
    void ensureCanView_managerCanViewAnEmployeeInTheSameCompany() {
        Employee manager = employee(1L, EmployeeRole.MANAGER, employerA);
        Employee target = employee(2L, EmployeeRole.EMPLOYEE, employerA);
        when(employeeRepository.findByEmail("manager@company.com")).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(target));

        assertDoesNotThrow(() -> accessService.ensureCanView("manager@company.com", 2L));
    }

    @Test
    void ensureCanView_cLevelCanViewAnEmployeeInTheSameCompany() {
        Employee cLevel = employee(1L, EmployeeRole.C_LEVEL_EXECUTIVE, employerA);
        Employee target = employee(2L, EmployeeRole.EMPLOYEE, employerA);
        when(employeeRepository.findByEmail("clevel@company.com")).thenReturn(Optional.of(cLevel));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(target));

        assertDoesNotThrow(() -> accessService.ensureCanView("clevel@company.com", 2L));
    }

    @Test
    void ensureCanView_managerCannotViewAnEmployeeAtADifferentCompany() {
        Employee manager = employee(1L, EmployeeRole.MANAGER, employerA);
        Employee target = employee(2L, EmployeeRole.EMPLOYEE, employerB);
        when(employeeRepository.findByEmail("manager@company.com")).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(target));

        assertThrows(ForbiddenOperationException.class,
                () -> accessService.ensureCanView("manager@company.com", 2L));
    }

    @Test
    void ensureCanView_unknownCallerRaisesIllegalArgument() {
        when(employeeRepository.findByEmail("ghost@company.com")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> accessService.ensureCanView("ghost@company.com", 1L));
    }

    @Test
    void ensureCanView_unknownTargetRaisesIllegalArgument() {
        Employee manager = employee(1L, EmployeeRole.MANAGER, employerA);
        when(employeeRepository.findByEmail("manager@company.com")).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> accessService.ensureCanView("manager@company.com", 99L));
    }

    @Test
    void ensureCanViewEngagement_delegatesToTheOwningEmployeesRecord() {
        Employee manager = employee(1L, EmployeeRole.MANAGER, employerA);
        Employee target = employee(2L, EmployeeRole.EMPLOYEE, employerA);
        Engagement engagement = new Engagement();
        engagement.setId(50L);
        engagement.setEmployee(target);

        when(engagementRepository.findById(50L)).thenReturn(Optional.of(engagement));
        when(employeeRepository.findByEmail("manager@company.com")).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(target));

        assertDoesNotThrow(() -> accessService.ensureCanViewEngagement("manager@company.com", 50L));
    }

    @Test
    void ensureCanViewEngagement_selfReadIsAllowedRegardlessOfRole_soAnEmployeesOwnTermsAreNeverBlocked() {
        // Regression coverage: a blanket role gate on engagement/term reads once broke every
        // plain employee's own entitlement summary. This asserts self-access always succeeds,
        // independent of the caller's role.
        Employee plainEmployee = employee(3L, EmployeeRole.EMPLOYEE, employerA);
        Engagement engagement = new Engagement();
        engagement.setId(51L);
        engagement.setEmployee(plainEmployee);

        when(engagementRepository.findById(51L)).thenReturn(Optional.of(engagement));
        when(employeeRepository.findByEmail("employee@company.com")).thenReturn(Optional.of(plainEmployee));

        assertDoesNotThrow(() -> accessService.ensureCanViewEngagement("employee@company.com", 51L));
        verify(employeeRepository, never()).findById(anyLong());
    }

    @Test
    void ensureCanViewEngagement_unknownEngagementRaisesIllegalArgument() {
        when(engagementRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> accessService.ensureCanViewEngagement("someone@company.com", 404L));
    }
}
