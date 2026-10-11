package com.fioneer.insurance.policy.model;

import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(
        name = "policy_insured_person",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_policy_insured_jmbg",
                columnNames = {"travel_details_id", "jmbg"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PolicyInsuredPerson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "travel_details_id", nullable = false)
    private TravelPolicyDetails travelDetails;

    @Column(name = "first_name", nullable = false, length = 50)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 50)
    private String lastName;

    @Column(nullable = false, length = 13)
    private String jmbg;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 20)
    private TravelerAgeGroup ageGroup;

    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;
}
