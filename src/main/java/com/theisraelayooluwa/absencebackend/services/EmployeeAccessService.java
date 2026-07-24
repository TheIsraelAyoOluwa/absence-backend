package com.theisraelayooluwa.absencebackend.services;

import com.theisraelayooluwa.absencebackend.exception.ForbiddenOperationException;
import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Engagement;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EngagementRepository;
import org.springframework.stereotype.Service;

@Service
public class EmployeeAccessService {

    private final EmployeeRepository employeeRepository;
    private final EngagementRepository engagementRepository;

    public EmployeeAccessService(EmployeeRepository employeeRepository, EngagementRepository engagementRepository) {
        this.employeeRepository = employeeRepository;
        this.engagementRepository = engagementRepository;
    }

    /**
     * Only an employee's own record is viewable by default; managers and C-level
     * executives may additionally view employees within their own company.
     */
    public void ensureCanView(String callerEmail, Long targetEmployeeId) {
        Employee caller = employeeRepository.findByEmail(callerEmail)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + callerEmail));

        if (caller.getId().equals(targetEmployeeId)) {
            return;
        }
        if (!caller.canApproveLeave()) {
            throw new ForbiddenOperationException(
                    "Only managers or C-level executives can view other employees' information");
        }

        Employee target = employeeRepository.findById(targetEmployeeId)
                .orElseThrow(() -> new IllegalArgumentException("Employee not found: " + targetEmployeeId));

        Long callerEmployerId = caller.getEmployer() != null ? caller.getEmployer().getId() : null;
        Long targetEmployerId = target.getEmployer() != null ? target.getEmployer().getId() : null;
        if (callerEmployerId == null || !callerEmployerId.equals(targetEmployerId)) {
            throw new ForbiddenOperationException("You can only view employees within your own company");
        }
    }

    /**
     * Same rule as {@link #ensureCanView}, applied to an Engagement rather than
     * an Employee id directly: viewable if it's the caller's own, otherwise
     * only a manager/C-level within the same company may view it.
     */
    public void ensureCanViewEngagement(String callerEmail, Long engagementId) {
        Engagement engagement = engagementRepository.findById(engagementId)
                .orElseThrow(() -> new IllegalArgumentException("Engagement not found: " + engagementId));
        ensureCanView(callerEmail, engagement.getEmployee().getId());
    }
}
