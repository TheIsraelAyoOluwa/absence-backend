package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.WorkPattern;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkPatternRepository extends JpaRepository<WorkPattern, Long> {
}
