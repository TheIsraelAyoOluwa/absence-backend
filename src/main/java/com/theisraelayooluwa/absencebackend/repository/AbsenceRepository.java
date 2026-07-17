package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.Absence;
import com.theisraelayooluwa.absencebackend.model.AbsenceType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AbsenceRepository extends JpaRepository<Absence, Long> {
    List<Absence> findByEmployeeId(Long employeeId);

    List<Absence> findByEmployeeIdAndType(Long employeeId, AbsenceType type);

    List<Absence> findByEmployeeIdAndStartDateBetween(Long employeeId, LocalDate from, LocalDate to);

    List<Absence> findByType(AbsenceType type);
}
