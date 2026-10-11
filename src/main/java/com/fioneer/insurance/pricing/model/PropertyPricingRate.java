package com.fioneer.insurance.pricing.model;

import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(
        name = "property_pricing_rate",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_property_rate_package",
                columnNames = "package_id"
        )
)
@Getter
@Setter
@NoArgsConstructor
public class PropertyPricingRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false, unique = true)
    private InsurancePackage insurancePackage;

    @Column(name = "price_per_square_meter", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerSquareMeter;

    @Column(nullable = false, length = 3)
    private String currency;
}
