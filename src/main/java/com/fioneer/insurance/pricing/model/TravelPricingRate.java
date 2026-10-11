package com.fioneer.insurance.pricing.model;

import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import com.fioneer.insurance.policy.model.enums.TravelRegion;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "travel_pricing_rate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_travel_rate_package_region_age",
                columnNames = {"package_id", "region", "age_group"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class TravelPricingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private InsurancePackage insurancePackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TravelRegion region;

    @Enumerated(EnumType.STRING)
    @Column(name = "age_group", nullable = false, length = 20)
    private TravelerAgeGroup ageGroup;

    @Column(name = "daily_rate", nullable = false, precision = 12, scale = 2)
    private BigDecimal dailyRate;

    @Column(nullable = false, length = 3)
    private String currency;
}
