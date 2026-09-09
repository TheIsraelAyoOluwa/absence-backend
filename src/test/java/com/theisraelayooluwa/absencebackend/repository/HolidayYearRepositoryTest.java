package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.model.Engagement;
import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Exercises the custom {@code findContaining} JPQL query against a real
 * (in-memory H2) database, rather than trusting the query text is correct
 * by inspection alone - this is the query {@code AbsenceService} relies on
 * to find the Holiday Year an absence request falls within.
 */
@DataJpaTest
class HolidayYearRepositoryTest {

    @Autowired
    private HolidayYearRepository holidayYearRepository;
    @Autowired
    private EmployeeRepository employeeRepository;
    @Autowired
    private EmployerRepository employerRepository;
    @Autowired
    private EngagementRepository engagementRepository;

    private Long seedEngagementWithTwoConsecutiveHolidayYears() {
        Employer employer = new Employer();
        employer.setName("Acme Ltd");
        employer.setEmail("hr+" + System.nanoTime() + "@acme.com");
        employer.setPasswordHash("hash");
        employer = employerRepository.save(employer);

        Employee employee = new Employee();
        employee.setFirstName("Ava");
        employee.setLastName("Smith");
        employee.setPayrollNumber("EMP-" + System.nanoTime());
        employee.setEmail("ava+" + System.nanoTime() + "@acme.com");
        employee.setEmployer(employer);
        employee.setPasswordHash("hash");
        employee = employeeRepository.save(employee);

        Engagement engagement = new Engagement();
        engagement.setEmployee(employee);
        engagement.setEmployer(employer);
        engagement.setStartDate(LocalDate.of(2020, 1, 1));
        engagement = engagementRepository.save(engagement);

        HolidayYear year2020 = new HolidayYear();
        year2020.setEngagement(engagement);
        year2020.setStartDate(LocalDate.of(2020, 1, 1));
        year2020.setEndDate(LocalDate.of(2020, 12, 31));
        holidayYearRepository.save(year2020);

        HolidayYear year2021 = new HolidayYear();
        year2021.setEngagement(engagement);
        year2021.setStartDate(LocalDate.of(2021, 1, 1));
        year2021.setEndDate(LocalDate.of(2021, 12, 31));
        holidayYearRepository.save(year2021);

        return engagement.getId();
    }

    @Test
    void findContaining_returnsTheHolidayYearThatSpansTheGivenDate() {
        Long engagementId = seedEngagementWithTwoConsecutiveHolidayYears();

        Optional<HolidayYear> found = holidayYearRepository.findContaining(engagementId, LocalDate.of(2020, 7, 1));

        assertTrue(found.isPresent());
        assertEquals(LocalDate.of(2020, 1, 1), found.get().getStartDate());
        assertEquals(LocalDate.of(2020, 12, 31), found.get().getEndDate());
    }

    @Test
    void findContaining_picksTheCorrectYearWhenMultipleExistOnTheSameEngagement() {
        Long engagementId = seedEngagementWithTwoConsecutiveHolidayYears();

        Optional<HolidayYear> found = holidayYearRepository.findContaining(engagementId, LocalDate.of(2021, 3, 15));

        assertTrue(found.isPresent());
        assertEquals(LocalDate.of(2021, 1, 1), found.get().getStartDate());
    }

    @Test
    void findContaining_isEmptyForADateOutsideEveryHolidayYear() {
        Long engagementId = seedEngagementWithTwoConsecutiveHolidayYears();

        Optional<HolidayYear> found = holidayYearRepository.findContaining(engagementId, LocalDate.of(2022, 1, 1));

        assertTrue(found.isEmpty());
    }

    @Test
    void findContaining_treatsBothBoundaryDatesAsInclusive() {
        Long engagementId = seedEngagementWithTwoConsecutiveHolidayYears();

        assertTrue(holidayYearRepository.findContaining(engagementId, LocalDate.of(2020, 1, 1)).isPresent());
        assertTrue(holidayYearRepository.findContaining(engagementId, LocalDate.of(2020, 12, 31)).isPresent());
    }
}
