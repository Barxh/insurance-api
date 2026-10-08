package com.fioneer.insurance.insurance_package.model;

import com.fioneer.insurance.insurance_product.model.InsuranceProduct;
import com.fioneer.insurance.insurance_coverage.model.InsuranceCoverage;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "insurance_packages",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_insurance_package_product_name",
                        columnNames = {"product_id", "name"}
                )
        }
)
public class InsurancePackage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(
            mappedBy = "insurancePackage",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<InsuranceCoverage> coverages = new ArrayList<>();
}