package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.Engagement;
import com.theisraelayooluwa.absencebackend.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EngagementRepository extends JpaRepository<Engagement, Long> {
    List<Engagement> findByEmployeeId(Long employeeId);

    List<Engagement> findByEmployerId(Long employerId);

    @Query("SELECT DISTINCT e.employee FROM Engagement e WHERE e.employer.id = :employerId")
    List<Employee> findEmployeesByEmployerId(@Param("employerId") Long employerId);
}
