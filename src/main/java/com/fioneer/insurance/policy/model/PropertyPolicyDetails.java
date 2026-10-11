package com.fioneer.insurance.policy.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "property_policy_details")
@Getter
@Setter
@NoArgsConstructor
public class PropertyPolicyDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false, unique = true)
    private Policy policy;

    @Column(name = "area_sqm", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaSquareMeters;

    @Column(name = "rate_per_square_meter", nullable = false, precision = 12, scale = 2)
    private BigDecimal ratePerSquareMeter;
}
