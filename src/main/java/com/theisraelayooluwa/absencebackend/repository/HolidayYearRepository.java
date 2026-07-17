package com.theisraelayooluwa.absencebackend.repository;

import com.theisraelayooluwa.absencebackend.model.HolidayYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HolidayYearRepository extends JpaRepository<HolidayYear, Long> {
    List<HolidayYear> findByEngagementIdOrderByStartDateAsc(Long engagementId);

    @Query("select hy from HolidayYear hy where hy.engagement.id = :engagementId " +
            "and hy.startDate <= :date and hy.endDate >= :date")
    Optional<HolidayYear> findContaining(Long engagementId, LocalDate date);
}
