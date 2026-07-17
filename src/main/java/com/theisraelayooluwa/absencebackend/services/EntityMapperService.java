package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.dto.EmployeeDirectoryDto;
import com.theisraelayooluwa.absencebackend.dto.EmployerDto;
import com.theisraelayooluwa.absencebackend.dto.EmployerOnboardingDto;
import com.theisraelayooluwa.absencebackend.dto.HolidayYearDto;
import com.theisraelayooluwa.absencebackend.dto.TermDto;
import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import com.theisraelayooluwa.absencebackend.model.Term;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntityMapperService {

    // Apply EmployerDto (update) fields to Employer entity, skipping null values
    public void mapEmployerDto(EmployerDto dto, Employer employer) {
        employer.setName(dto.name());
        if (dto.workingCriteria() != null) {
            employer.setWorkingCriteria(dto.workingCriteria());
        }
        if (dto.standardDailyHours() != null) {
            employer.setStandardDailyHours(dto.standardDailyHours());
        }
        if (dto.fiscalYearStartMonth() != null) {
            employer.setFiscalYearStartMonth(dto.fiscalYearStartMonth());
        }
        if (dto.fiscalYearStartDay() != null) {
            employer.setFiscalYearStartDay(dto.fiscalYearStartDay());
        }
        if (dto.fiscalYearEndMonth() != null) {
            employer.setFiscalYearEndMonth(dto.fiscalYearEndMonth());
        }
        if (dto.fiscalYearEndDay() != null) {
            employer.setFiscalYearEndDay(dto.fiscalYearEndDay());
        }
        if (dto.publicHolidayRegion() != null) {
            employer.setPublicHolidayRegion(dto.publicHolidayRegion());
        }
        if (dto.publicHolidaysIncludedInEntitlement() != null) {
            employer.setPublicHolidaysIncludedInEntitlement(dto.publicHolidaysIncludedInEntitlement());
        }
    }

    // Apply EmployerOnboardingDto (signup) fields to Employer entity, skipping null values
    public void mapEmployerOnboardingDto(EmployerOnboardingDto dto, Employer employer) {
        employer.setName(dto.name());
        if (dto.workingCriteria() != null) {
            employer.setWorkingCriteria(dto.workingCriteria());
        }
        if (dto.standardDailyHours() != null) {
            employer.setStandardDailyHours(dto.standardDailyHours());
        }
        if (dto.fiscalYearStartMonth() != null) {
            employer.setFiscalYearStartMonth(dto.fiscalYearStartMonth());
        }
        if (dto.fiscalYearStartDay() != null) {
            employer.setFiscalYearStartDay(dto.fiscalYearStartDay());
        }
        if (dto.fiscalYearEndMonth() != null) {
            employer.setFiscalYearEndMonth(dto.fiscalYearEndMonth());
        }
        if (dto.fiscalYearEndDay() != null) {
            employer.setFiscalYearEndDay(dto.fiscalYearEndDay());
        }
        if (dto.publicHolidayRegion() != null) {
            employer.setPublicHolidayRegion(dto.publicHolidayRegion());
        }
        if (dto.publicHolidaysIncludedInEntitlement() != null) {
            employer.setPublicHolidaysIncludedInEntitlement(dto.publicHolidaysIncludedInEntitlement());
        }
    }

    // Apply TermDto fields to Term entity, skipping null values
    public void mapTermDto(TermDto dto, Term term) {
        if (dto.annualEntitlementHours() != null) {
            term.setAnnualEntitlementHours(dto.annualEntitlementHours());
        }
        if (dto.roundingPolicy() != null) {
            term.setRoundingPolicy(dto.roundingPolicy());
        }
        if (dto.carriedOverHours() != null) {
            term.setCarriedOverHours(dto.carriedOverHours());
        }
    }

    // Apply HolidayYearDto fields to HolidayYear entity, skipping null values
    public void mapHolidayYearDto(HolidayYearDto dto, HolidayYear holidayYear) {
        if (dto.carryOverLimitHours() != null) {
            holidayYear.setCarryOverLimitHours(dto.carryOverLimitHours());
        }
        if (dto.carriedOverHours() != null) {
            holidayYear.setCarriedOverHours(dto.carriedOverHours());
        }
        if (dto.extraHolidayHours() != null) {
            holidayYear.setExtraHolidayHours(dto.extraHolidayHours());
        }
    }

    // Convert Employee entities to EmployeeDirectoryDto (unified response format)
    public List<EmployeeDirectoryDto> toEmployeeDirectoryDtos(List<Employee> employees) {
        return employees.stream().map(EmployeeDirectoryDto::from).toList();
    }
}
