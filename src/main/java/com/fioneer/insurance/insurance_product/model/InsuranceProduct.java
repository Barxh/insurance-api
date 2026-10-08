package com.fioneer.insurance.insurance_product.model;

import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "insurance_products",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_insurance_product_name",
                        columnNames = "name"
                )
        }
)
public class InsuranceProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @OneToMany(
            mappedBy = "product",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<InsurancePackage> packages = new ArrayList<>();
}