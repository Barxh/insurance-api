package com.fioneer.insurance.pricing.model;

import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.policy.model.enums.VehicleType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "roadside_pricing_rate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_roadside_rate_package_vehicle",
                columnNames = {"package_id", "vehicle_type"}
        )
)
@Getter
@Setter
@NoArgsConstructor
public class RoadsidePricingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private InsurancePackage insurancePackage;

    @Enumerated(EnumType.STRING)
    @Column(name = "vehicle_type", nullable = false, length = 20)
    private VehicleType vehicleType;

    @Column(name = "annual_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal annualPrice;

    @Column(nullable = false, length = 3)
    private String currency;
}
