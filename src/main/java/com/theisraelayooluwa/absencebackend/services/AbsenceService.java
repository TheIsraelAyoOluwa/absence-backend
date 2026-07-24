package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.exception.ForbiddenOperationException;
import com.theisraelayooluwa.absencebackend.model.*;
import com.theisraelayooluwa.absencebackend.repository.AbsenceRepository;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EngagementRepository;
import com.theisraelayooluwa.absencebackend.repository.HolidayYearRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;


/**
 * Implements the absence use cases from slides 2-4:
 *  - Record sickness / unexplained absence (Employee or Employer)
 *  - Request & approve holiday / other leave / scheduled training
 *  - Enforce the Planned vs Unplanned entry rules from slide 5
 */



@Service
public class AbsenceService {


    private final AbsenceRepository absenceRepository;
    private final EmployeeRepository employeeRepository;
    private final EngagementRepository engagementRepository;
    private final HolidayYearRepository holidayYearRepository;
    private final EntitlementService entitlementService;



    public AbsenceService(AbsenceRepository absenceRepository,
                          EmployeeRepository employeeRepository,
                          EngagementRepository engagementRepository,
                          HolidayYearRepository holidayYearRepository,
                          EntitlementService entitlementService) {
        this.absenceRepository = absenceRepository;
        this.employeeRepository = employeeRepository;
        this.engagementRepository = engagementRepository;
        this.holidayYearRepository = holidayYearRepository;
        this.entitlementService = entitlementService;
    }



    /**
     * Request a Planned absence (Holiday / Other Leave / Scheduled Training).
     * Per slide 5, planned absence is usually entered before the date and
     * requires manager approval, so it starts life as REQUESTED.
     */


    @Transactional
    public Absence recordAbsence(Long employeeId, AbsenceType type, LocalDate start, LocalDate end,
                                String otherLeaveCategory, Absence.RecordedBy recordedBy, String notes) {
       Employee employee = getEmployee(employeeId);

       if (type.isPlanned()) {
           if (type == AbsenceType.HOLIDAY) {
               validateSufficientHolidayEntitlement(employee, start, end);
           }
            
           Absence absence = new Absence();
           absence.setEmployee(employee);
           absence.setType(type);
           absence.setStatus(AbsenceStatus.REQUESTED);
           absence.setStartDate(start);
           absence.setEndDate(end);
           absence.setDurationHours(calculateWorkingHours(employee, start, end).doubleValue());
           absence.setRecordedBy(Absence.RecordedBy.EMPLOYEE);
           absence.setOtherLeaveCategory(otherLeaveCategory);
           absence.setNotes(notes);
            
           return absenceRepository.save(absence);
       } else {
           Absence absence = new Absence();
           absence.setEmployee(employee);
           absence.setType(type);
           absence.setStatus(AbsenceStatus.RECORDED);
           absence.setStartDate(start);
           absence.setEndDate(end);
           absence.setDurationHours(calculateWorkingHours(employee, start, end).doubleValue());
           absence.setRecordedBy(recordedBy);
           absence.setNotes(notes);
            
           return absenceRepository.save(absence);
       }
    }

    @Transactional
    public Absence processLeaveDecision(Long absenceId, String decision, String approverEmail) {
       ensureApproverCanApproveLeave(approverEmail);
       Absence absence = getAbsence(absenceId);
        
       if (absence.getStatus() != AbsenceStatus.REQUESTED) {
           throw new IllegalStateException("Only REQUESTED absences can be approved or rejected");
       }

       if ("APPROVE".equalsIgnoreCase(decision)) {
           absence.setStatus(AbsenceStatus.APPROVED);
       } else if ("REJECT".equalsIgnoreCase(decision)) {
           absence.setStatus(AbsenceStatus.REJECTED);
       } else {
           throw new IllegalArgumentException("Invalid decision: " + decision + ". Must be APPROVE or REJECT");
       }

       return absenceRepository.save(absence);
    }

    public List<Absence> getAbsencesForEmployee(Long employeeId) {
        return absenceRepository.findByEmployeeId(employeeId);
    }

    public Absence getAbsenceById(Long absenceId) {
        return getAbsence(absenceId);
    }



    /**
     * Working days consumed by an absence, derived from the employee's currently
     * active work pattern - only days the employee is normally scheduled to work
     * count towards the absence duration.
     */
    public BigDecimal calculateWorkingHours(Employee employee, LocalDate start, LocalDate end) {
        WorkPattern pattern = findActiveWorkPattern(employee, start);
        BigDecimal hours = BigDecimal.ZERO;
        LocalDate d = start;
        while (!d.isAfter(end)) {
            Double dayHours = pattern.getDayHours().get(d.getDayOfWeek());
            if (dayHours != null && dayHours > 0) {
                hours = hours.add(BigDecimal.valueOf(dayHours));
            }
            d = d.plusDays(1);
        }
        return hours.setScale(2, java.math.RoundingMode.HALF_UP);
    }



    private WorkPattern findActiveWorkPattern(Employee employee, LocalDate onDate) {
        return engagementRepository.findByEmployeeId(employee.getId()).stream()
                .flatMap(e -> e.getTerms().stream())
                .filter(t -> !t.getStartDate().isAfter(onDate)
                        && (t.getEndDate() == null || !t.getEndDate().isBefore(onDate)))
                .findFirst()
                .map(Term::getWorkPattern)
                .orElseThrow(() -> new IllegalStateException(
                        "No active Term/WorkPattern found for employee " + employee.getId() + " on " + onDate));
    }

    private void validateSufficientHolidayEntitlement(Employee employee, LocalDate start, LocalDate end) {
        Optional<Engagement> engagement = engagementRepository.findByEmployeeId(employee.getId()).stream()
                .filter(Engagement::isActive)
                .findFirst();

        if (engagement.isEmpty()) {
            return; // no active engagement to validate against
        }

        Optional<HolidayYear> holidayYear = holidayYearRepository.findContaining(engagement.get().getId(), start);
        Optional<Term> term = engagement.get().getTerms().stream()
                .filter(t -> !t.getStartDate().isAfter(start) && (t.getEndDate() == null || !t.getEndDate().isBefore(start)))
                .findFirst();

        if (holidayYear.isEmpty() || term.isEmpty()) {
            return; // insufficient data to validate - allow the request through
        }

        BigDecimal available = entitlementService.calculateTotalAvailableHours(term.get(), holidayYear.get());
        BigDecimal alreadyTaken = absenceRepository.findByEmployeeIdAndType(employee.getId(), AbsenceType.HOLIDAY).stream()
                .filter(a -> a.getStatus() == AbsenceStatus.APPROVED || a.getStatus() == AbsenceStatus.REQUESTED)
                .filter(a -> holidayYear.get().contains(a.getStartDate()))
                .map(a -> a.getDurationHours() != null ? BigDecimal.valueOf(a.getDurationHours()) : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal requested = calculateWorkingHours(employee, start, end);

        if (alreadyTaken.add(requested).compareTo(available) > 0) {
            throw new IllegalStateException(String.format(
                    "Insufficient holiday entitlement: %.2f hours available, %.2f already taken/requested, %.2f requested",
                    available.doubleValue(), alreadyTaken.doubleValue(), requested.doubleValue()));
        }
    }

    private Employee getEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + id));
    }

    private Absence getAbsence(Long id) {
        return absenceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Absence not found: " + id));
    }

    private void ensureApproverCanApproveLeave(String approverEmail) {
        Employee approver = employeeRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + approverEmail));
        if (!approver.canApproveLeave()) {
            throw new ForbiddenOperationException("Only managers or C-level executives can approve or reject leave");
        }
    }








}
