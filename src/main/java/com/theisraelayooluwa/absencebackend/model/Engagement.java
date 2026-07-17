package com.theisraelayooluwa.absencebackend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;



@Entity
@Table(name = "engagements")
@Getter
@Setter
@NoArgsConstructor
public class Engagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "employer_id")
    private Employer employer;

    @Column(nullable = false)
    private LocalDate startDate;

    /** Null while the engagement is ongoing. */
    private LocalDate endDate;

    @JsonIgnore
    @OneToMany(mappedBy = "engagement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Term> terms = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "engagement", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HolidayYear> holidayYears = new ArrayList<>();

    public boolean isActive() {
        return endDate == null || !endDate.isBefore(LocalDate.now());
    }
}
