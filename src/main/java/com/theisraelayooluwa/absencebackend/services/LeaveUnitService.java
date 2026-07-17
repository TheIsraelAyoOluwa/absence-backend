package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Employer;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.DateTimeException;

@Service
public class LeaveUnitService {

    // Convert hours (internal storage format) to days for display
    public BigDecimal hoursToDays(BigDecimal hours, double hoursPerDay) {
        if (hoursPerDay <= 0) {
            return hours.setScale(2, RoundingMode.HALF_UP);
        }
        return hours.divide(BigDecimal.valueOf(hoursPerDay), 2, RoundingMode.HALF_UP);
    }

    // Convert days (user input) to hours (internal storage format)
    public BigDecimal daysToHours(BigDecimal days, double hoursPerDay) {
        return days.multiply(BigDecimal.valueOf(hoursPerDay)).setScale(2, RoundingMode.HALF_UP);
    }

    // Resolve employee's daily hours: employee override > employer standard > 8.0 default
    public double resolveDailyHours(Employee employee) {
        if (employee.getDailyHours() != null && employee.getDailyHours() > 0) {
            return employee.getDailyHours();
        }
        Employer employer = employee.getEmployer();
        if (employer != null && employer.getStandardDailyHours() != null && employer.getStandardDailyHours() > 0) {
            return employer.getStandardDailyHours();
        }
        return 8.0;
    }

    // Calculate fiscal year window for a given reference date using employer's custom fiscal year settings
    // E.g., if fiscal year is April 1 - March 31, returns the window containing the reference date
    public FiscalYearWindow resolveFiscalYearWindow(Employer employer, LocalDate referenceDate) {
        int startMonth = defaultInt(employer.getFiscalYearStartMonth(), 1);
        int startDay = defaultInt(employer.getFiscalYearStartDay(), 1);
        int endMonth = defaultInt(employer.getFiscalYearEndMonth(), 12);
        int endDay = defaultInt(employer.getFiscalYearEndDay(), 31);

        LocalDate start = buildDate(referenceDate.getYear(), startMonth, startDay);
        if (referenceDate.isBefore(start)) {
            start = start.minusYears(1);
        }

        LocalDate end = buildDate(start.getYear(), endMonth, endDay);
        if (end.isBefore(start)) {
            end = buildDate(start.getYear() + 1, endMonth, endDay);
        }

        return new FiscalYearWindow(start, end);
    }

    public record FiscalYearWindow(LocalDate startDate, LocalDate endDate) {
    }

    private int defaultInt(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private LocalDate buildDate(int year, int month, int day) {
        try {
            return MonthDay.of(month, day).atYear(year);
        } catch (DateTimeException ex) {
            throw new IllegalArgumentException("Invalid fiscal year date: " + month + "/" + day, ex);
        }
    }
}
