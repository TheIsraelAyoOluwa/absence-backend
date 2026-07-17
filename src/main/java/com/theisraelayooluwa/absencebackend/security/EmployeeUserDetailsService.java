package com.theisraelayooluwa.absencebackend.security;

import com.theisraelayooluwa.absencebackend.model.Employee;
import com.theisraelayooluwa.absencebackend.model.Employer;
import com.theisraelayooluwa.absencebackend.repository.EmployeeRepository;
import com.theisraelayooluwa.absencebackend.repository.EmployerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;
    private final EmployerRepository employerRepository;

    public EmployeeUserDetailsService(EmployeeRepository employeeRepository, EmployerRepository employerRepository) {
        this.employeeRepository = employeeRepository;
        this.employerRepository = employerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Try employer authentication first (by email)
        Employer employer = employerRepository.findByEmail(username).orElse(null);
        if (employer != null) {
          if (employer.getPasswordHash() == null || employer.getPasswordHash().isBlank()) {
               throw new UsernameNotFoundException("Employer has no password set: " + username);
           }
           return new User(
                   employer.getEmail(),
                   employer.getPasswordHash(),
                   List.of(new SimpleGrantedAuthority("ROLE_EMPLOYER"))
           );
        }

        // Fall back to employee authentication (by email or payroll number)
        Employee employee = employeeRepository.findByEmailOrPayrollNumber(username, username)
               .orElseThrow(() -> new UsernameNotFoundException("Employee not found: " + username));

        if (employee.getPasswordHash() == null || employee.getPasswordHash().isBlank()) {
           throw new UsernameNotFoundException("Employee has no password set: " + username);
        }
        // Employees can only login if approved by employer (prevents access for pending/rejected employees)
        if (employee.getApprovalStatus() != Employee.ApprovalStatus.APPROVED) {
           throw new UsernameNotFoundException("Employee account is not approved: " + username);
        }

        return new User(
               employee.getEmail() != null ? employee.getEmail() : employee.getPayrollNumber(),
               employee.getPasswordHash(),
               List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()))
        );
    }
}
