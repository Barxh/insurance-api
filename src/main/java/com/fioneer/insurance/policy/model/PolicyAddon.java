package com.fioneer.insurance.policy.model;

import com.fioneer.insurance.insurance_addon.model.InsuranceAddon;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "policy_addon")
public class PolicyAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addon_id", nullable = false)
    private InsuranceAddon addon;

    @Column(name = "addon_name", nullable = false, length = 100)
    private String addonName;

    @Column(name = "addon_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal addonPrice;

    @Column(name = "addon_currency", nullable = false, length = 3)
    private String addonCurrency;
}
