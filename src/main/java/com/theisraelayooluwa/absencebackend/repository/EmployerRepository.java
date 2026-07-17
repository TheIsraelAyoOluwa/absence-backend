package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.Employer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmployerRepository extends JpaRepository<Employer, Long> {
    Optional<Employer> findFirstByNameIgnoreCaseOrderByIdAsc(String name);
    Optional<Employer> findByEmail(String email);
}
