package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.WorkPattern;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkPatternRepository extends JpaRepository<WorkPattern, Long> {
    List<WorkPattern> findByEmployerIdOrderByDescriptionAsc(Long employerId);
}
