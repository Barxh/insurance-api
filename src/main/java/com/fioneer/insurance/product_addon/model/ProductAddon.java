package com.fioneer.insurance.product_addon.model;

import com.fioneer.insurance.insurance_addon.model.InsuranceAddon;
import com.fioneer.insurance.insurance_product.model.InsuranceProduct;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "product_addon",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_product_addon",
                        columnNames = {"product_id", "addon_id"}
                )
        }
)
public class ProductAddon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private InsuranceProduct product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "addon_id", nullable = false)
    private InsuranceAddon addon;
}