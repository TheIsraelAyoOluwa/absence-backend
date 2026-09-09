package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.exception.ForbiddenOperationException;
import com.theisraelayooluwa.absencebackend.model.*;
import com.theisraelayooluwa.absencebackend.repository.AbsenceRepository;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EngagementRepository;
import com.theisraelayooluwa.absencebackend.repository.HolidayYearRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbsenceServiceTest {

    @Mock
    private AbsenceRepository absenceRepository;
    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private EngagementRepository engagementRepository;
    @Mock
    private HolidayYearRepository holidayYearRepository;

    // Real instance: EntitlementService is pure/stateless, so exercising the actual
    // maths end to end is more useful here than mocking it out.
    private final EntitlementService entitlementService = new EntitlementService();

    private AbsenceService absenceService;

    private Employee employee;
    private Engagement engagement;
    private Term term;
    private HolidayYear holidayYear;

    @BeforeEach
    void setUp() {
        absenceService = new AbsenceService(absenceRepository, employeeRepository, engagementRepository,
                holidayYearRepository, entitlementService);

        employee = new Employee();
        employee.setId(1L);
        employee.setRole(EmployeeRole.EMPLOYEE);

        WorkPattern pattern = new WorkPattern();
        Map<DayOfWeek, Double> days = new EnumMap<>(DayOfWeek.class);
        days.put(DayOfWeek.MONDAY, 8.0);
        days.put(DayOfWeek.TUESDAY, 8.0);
        days.put(DayOfWeek.WEDNESDAY, 8.0);
        days.put(DayOfWeek.THURSDAY, 8.0);
        days.put(DayOfWeek.FRIDAY, 8.0);
        pattern.setDayHours(days);

        engagement = new Engagement();
        engagement.setId(10L);
        engagement.setEmployee(employee);

        term = new Term();
        term.setId(100L);
        term.setEngagement(engagement);
        term.setWorkPattern(pattern);
        term.setStartDate(LocalDate.of(2020, 1, 1));
        term.setEndDate(null);
        term.setAnnualEntitlementHours(224.0);
        term.setRoundingPolicy(RoundingPolicy.NO_ROUNDING);
        engagement.setTerms(List.of(term));

        holidayYear = new HolidayYear();
        holidayYear.setId(1000L);
        holidayYear.setEngagement(engagement);
        holidayYear.setStartDate(LocalDate.of(2020, 1, 1));
        holidayYear.setEndDate(LocalDate.of(2020, 12, 31));
        holidayYear.setCarryOverLimitHours(0.0);
        holidayYear.setCarriedOverHours(0.0);
        holidayYear.setExtraHolidayHours(0.0);

        // lenient: not every test in this class reaches a save() call (several assert an
        // exception is thrown first), and that's fine - this stub just needs to be here
        // for the tests that do.
        lenient().when(absenceRepository.save(any(Absence.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void calculateWorkingHours_onlyCountsDaysTheEmployeeIsScheduledToWork() {
        // Monday 1 June 2020 through Sunday 7 June 2020: five scheduled weekdays, one weekend.
        when(engagementRepository.findByEmployeeId(1L)).thenReturn(List.of(engagement));

        BigDecimal hours = absenceService.calculateWorkingHours(employee,
                LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 7));

        assertEquals(0, BigDecimal.valueOf(40.0).compareTo(hours));
    }

    @Test
    void recordAbsence_plannedHoliday_withSufficientEntitlement_isSavedAsRequested() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(engagementRepository.findByEmployeeId(1L)).thenReturn(List.of(engagement));
        when(holidayYearRepository.findContaining(10L, LocalDate.of(2020, 6, 1)))
                .thenReturn(Optional.of(holidayYear));
        when(absenceRepository.findByEmployeeIdAndType(1L, AbsenceType.HOLIDAY)).thenReturn(List.of());

        Absence result = absenceService.recordAbsence(1L, AbsenceType.HOLIDAY,
                LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 5), null, Absence.RecordedBy.EMPLOYEE, "Summer break");

        assertEquals(AbsenceStatus.REQUESTED, result.getStatus());
        assertEquals(Absence.RecordedBy.EMPLOYEE, result.getRecordedBy());
        assertEquals(40.0, result.getDurationHours());
    }

    @Test
    void recordAbsence_plannedHoliday_exceedingEntitlement_isRejected() {
        term.setAnnualEntitlementHours(10.0); // deliberately too little for the request below

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(engagementRepository.findByEmployeeId(1L)).thenReturn(List.of(engagement));
        when(holidayYearRepository.findContaining(10L, LocalDate.of(2020, 6, 1)))
                .thenReturn(Optional.of(holidayYear));
        when(absenceRepository.findByEmployeeIdAndType(1L, AbsenceType.HOLIDAY)).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> absenceService.recordAbsence(1L, AbsenceType.HOLIDAY,
                LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 5), null, Absence.RecordedBy.EMPLOYEE, null));
    }

    @Test
    void recordAbsence_plannedHoliday_accountsForAbsenceAlreadyTakenThisHolidayYear() {
        term.setAnnualEntitlementHours(48.0); // exactly one working week

        Absence existing = new Absence();
        existing.setEmployee(employee);
        existing.setType(AbsenceType.HOLIDAY);
        existing.setStatus(AbsenceStatus.APPROVED);
        existing.setStartDate(LocalDate.of(2020, 3, 2));
        existing.setEndDate(LocalDate.of(2020, 3, 2));
        existing.setDurationHours(40.0); // most of the entitlement already used

        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(engagementRepository.findByEmployeeId(1L)).thenReturn(List.of(engagement));
        when(holidayYearRepository.findContaining(10L, LocalDate.of(2020, 6, 1)))
                .thenReturn(Optional.of(holidayYear));
        when(absenceRepository.findByEmployeeIdAndType(1L, AbsenceType.HOLIDAY)).thenReturn(List.of(existing));

        // Only 8 hours remain; requesting a further 40 must be rejected.
        assertThrows(IllegalStateException.class, () -> absenceService.recordAbsence(1L, AbsenceType.HOLIDAY,
                LocalDate.of(2020, 6, 1), LocalDate.of(2020, 6, 5), null, Absence.RecordedBy.EMPLOYEE, null));
    }

    @Test
    void recordAbsence_sickness_isSavedAsRecordedRatherThanRequested() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(engagementRepository.findByEmployeeId(1L)).thenReturn(List.of(engagement));

        Absence result = absenceService.recordAbsence(1L, AbsenceType.SICKNESS,
                LocalDate.of(2020, 6, 2), LocalDate.of(2020, 6, 2), null, Absence.RecordedBy.EMPLOYER, "Flu");

        // Unplanned absence needs no manager approval step, so it's recorded directly.
        assertEquals(AbsenceStatus.RECORDED, result.getStatus());
        assertEquals(Absence.RecordedBy.EMPLOYER, result.getRecordedBy());
    }

    @Test
    void processLeaveDecision_managerApprovingAPendingRequest_succeeds() {
        Employee manager = new Employee();
        manager.setId(2L);
        manager.setRole(EmployeeRole.MANAGER);

        Absence absence = new Absence();
        absence.setId(500L);
        absence.setStatus(AbsenceStatus.REQUESTED);

        when(employeeRepository.findByEmail("manager@company.com")).thenReturn(Optional.of(manager));
        when(absenceRepository.findById(500L)).thenReturn(Optional.of(absence));

        Absence result = absenceService.processLeaveDecision(500L, "APPROVE", "manager@company.com");

        assertEquals(AbsenceStatus.APPROVED, result.getStatus());
    }

    @Test
    void processLeaveDecision_managerRejectingAPendingRequest_succeeds() {
        Employee manager = new Employee();
        manager.setId(2L);
        manager.setRole(EmployeeRole.C_LEVEL_EXECUTIVE);

        Absence absence = new Absence();
        absence.setId(501L);
        absence.setStatus(AbsenceStatus.REQUESTED);

        when(employeeRepository.findByEmail("clevel@company.com")).thenReturn(Optional.of(manager));
        when(absenceRepository.findById(501L)).thenReturn(Optional.of(absence));

        Absence result = absenceService.processLeaveDecision(501L, "reject", "clevel@company.com");

        assertEquals(AbsenceStatus.REJECTED, result.getStatus());
    }

    @Test
    void processLeaveDecision_byAPlainEmployee_isForbidden() {
        Employee plainEmployee = new Employee();
        plainEmployee.setId(3L);
        plainEmployee.setRole(EmployeeRole.EMPLOYEE);

        when(employeeRepository.findByEmail("employee@company.com")).thenReturn(Optional.of(plainEmployee));

        assertThrows(ForbiddenOperationException.class,
                () -> absenceService.processLeaveDecision(500L, "APPROVE", "employee@company.com"));
    }

    @Test
    void processLeaveDecision_onAnAlreadyDecidedAbsence_throwsIllegalState() {
        Employee manager = new Employee();
        manager.setId(2L);
        manager.setRole(EmployeeRole.MANAGER);

        Absence absence = new Absence();
        absence.setId(502L);
        absence.setStatus(AbsenceStatus.APPROVED); // already decided

        when(employeeRepository.findByEmail("manager@company.com")).thenReturn(Optional.of(manager));
        when(absenceRepository.findById(502L)).thenReturn(Optional.of(absence));

        assertThrows(IllegalStateException.class,
                () -> absenceService.processLeaveDecision(502L, "APPROVE", "manager@company.com"));
    }

    @Test
    void processLeaveDecision_withAnUnrecognisedDecision_throwsIllegalArgument() {
        Employee manager = new Employee();
        manager.setId(2L);
        manager.setRole(EmployeeRole.MANAGER);

        Absence absence = new Absence();
        absence.setId(503L);
        absence.setStatus(AbsenceStatus.REQUESTED);

        when(employeeRepository.findByEmail("manager@company.com")).thenReturn(Optional.of(manager));
        when(absenceRepository.findById(503L)).thenReturn(Optional.of(absence));

        assertThrows(IllegalArgumentException.class,
                () -> absenceService.processLeaveDecision(503L, "MAYBE", "manager@company.com"));
    }
}
