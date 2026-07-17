package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import com.theisraelayooluwa.absencebackend.model.Term;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;



/**
 * Implements the entitlement rules from slides 6, 7, 10, 11, 12:
 *  - Statutory minimum of 28 days for a full-time PAYE employee
 *  - Annual entitlement is prorated when a Term starts or ends part-way
 *    through the Holiday Year (proration is by calendar days in-period)
 *  - A rounding policy (up / down / nearest) is then applied
 *  - Carried-over days (term-to-term, and capped year-to-year) and any
 *    discretionary Extra Holiday are then added on top
 */


@Service
public class EntitlementService {
    /**
     * Prorated + rounded holiday days a Term contributes to a given Holiday Year,
     * based on how much of the Holiday Year the Term actually overlaps.
     */
    public BigDecimal calculateProratedEntitlement(Term term, HolidayYear holidayYear) {
        LocalDate overlapStart = maxDate(term.getStartDate(), holidayYear.getStartDate());
        LocalDate termEnd = term.getEndDate() != null ? term.getEndDate() : holidayYear.getEndDate();
        LocalDate overlapEnd = minDate(termEnd, holidayYear.getEndDate());

        if (overlapEnd.isBefore(overlapStart)) {
            return BigDecimal.ZERO;
        }

        long overlapDays = ChronoUnit.DAYS.between(overlapStart, overlapEnd) + 1;
        long holidayYearLength = holidayYear.lengthInDays();

        BigDecimal fraction = BigDecimal.valueOf(overlapDays)
                .divide(BigDecimal.valueOf(holidayYearLength), 10, RoundingMode.HALF_UP);

        BigDecimal rawEntitlement = BigDecimal.valueOf(term.getAnnualEntitlementHours())
                .multiply(fraction);

        return term.getRoundingPolicy().apply(rawEntitlement);
    }


    /**
     * Total days an employee may take in this Holiday Year:
     * prorated Term entitlement(s) + carried-over days (capped) + extra holiday.
     */
    public BigDecimal calculateTotalAvailableHours(Term term, HolidayYear holidayYear) {
        BigDecimal prorated = calculateProratedEntitlement(term, holidayYear);
        BigDecimal carriedOver = BigDecimal.valueOf(
                Math.min(holidayYear.getCarriedOverHours(), holidayYear.getCarryOverLimitHours()));
        BigDecimal extra = BigDecimal.valueOf(holidayYear.getExtraHolidayHours());

        return prorated.add(carriedOver).add(extra).setScale(1, RoundingMode.HALF_UP);
    }

    /** Hours of unused holiday to forfeit when a Term ends part-way through a Holiday Year (slide 10). */
    public BigDecimal calculateForfeitedEntitlement(BigDecimal fullYearEntitlement, BigDecimal proratedEntitlement) {
        BigDecimal forfeited = fullYearEntitlement.subtract(proratedEntitlement);
        return forfeited.max(BigDecimal.ZERO);
    }

    private LocalDate maxDate(LocalDate a, LocalDate b) {
        return a.isAfter(b) ? a : b;
    }

    private LocalDate minDate(LocalDate a, LocalDate b) {
        return a.isBefore(b) ? a : b;
    }

}
