package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByPayrollNumber(String payrollNumber);

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmailOrPayrollNumber(String email, String payrollNumber);

    List<Employee> findByEmployerIdOrderByLastNameAsc(Long employerId);

    List<Employee> findByEmployerIdAndApprovalStatusOrderByLastNameAsc(Long employerId, Employee.ApprovalStatus approvalStatus);
}
