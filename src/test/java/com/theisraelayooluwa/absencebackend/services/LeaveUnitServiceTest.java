package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.model.Employee;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LeaveUnitServiceTest {

    private final LeaveUnitService leaveUnitService = new LeaveUnitService();

    @Test
    void convertsHoursToDaysUsingCustomDailyHours() {
        assertEquals(new BigDecimal("1.50"), leaveUnitService.hoursToDays(BigDecimal.valueOf(6), 4.0));
    }

    @Test
    void resolvesFiscalYearWindowFromEmployerSettings() {
        Employer employer = new Employer();
        employer.setFiscalYearStartMonth(4);
        employer.setFiscalYearStartDay(1);
        employer.setFiscalYearEndMonth(3);
        employer.setFiscalYearEndDay(31);

        LeaveUnitService.FiscalYearWindow window = leaveUnitService.resolveFiscalYearWindow(employer, LocalDate.of(2026, 7, 16));
        assertEquals(LocalDate.of(2026, 4, 1), window.startDate());
        assertEquals(LocalDate.of(2027, 3, 31), window.endDate());
    }

    @Test
    void resolvesDailyHoursFromEmployeeThenEmployerThenDefault() {
        Employee employee = new Employee();
        Employer employer = new Employer();
        employee.setEmployer(employer);
        assertEquals(8.0, leaveUnitService.resolveDailyHours(employee));

        employer.setStandardDailyHours(6.5);
        assertEquals(6.5, leaveUnitService.resolveDailyHours(employee));

        employee.setDailyHours(4.0);
        assertEquals(4.0, leaveUnitService.resolveDailyHours(employee));
    }
}
