package com.fioneer.insurance.insurance_coverage.model;

import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "insurance_coverage",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_insurance_coverage_package_name",
                        columnNames = {"package_id", "name"}
                )
        }
)
public class InsuranceCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private InsurancePackage insurancePackage;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(name = "limit_amount", precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "limit_currency", length = 3)
    private String limitCurrency;
}