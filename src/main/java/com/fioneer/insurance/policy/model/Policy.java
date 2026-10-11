package com.fioneer.insurance.policy.model;

import com.fioneer.insurance.insurance_product.model.InsuranceProduct;
import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.policy.model.enums.PolicyStatus;
import com.fioneer.insurance.user.model.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "package_id", nullable = false)
    private InsurancePackage insurancePackage;

    @Column(name = "package_name", nullable = false, length = 100)
    private String packageName;

    @Column(name = "package_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal packagePrice;

    @Column(name = "package_currency", nullable = false, length = 3)
    private String packageCurrency;

    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PolicyStatus status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private TravelPolicyDetails travelDetails;

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private PropertyPolicyDetails propertyDetails;

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private RoadsidePolicyDetails roadsideDetails;

    @OneToMany(
            mappedBy = "policy",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PolicyAddon> addons = new ArrayList<>();

    @OneToMany(
            mappedBy = "policy",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<PolicyCoverage> coverages = new ArrayList<>();

}
