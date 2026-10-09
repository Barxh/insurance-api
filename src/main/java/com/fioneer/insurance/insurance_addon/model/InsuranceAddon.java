package com.fioneer.insurance.insurance_addon.model;

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
        name = "insurance_addon",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_insurance_addon_name",
                        columnNames = "name"
                )
        }
)
public class InsuranceAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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
}
