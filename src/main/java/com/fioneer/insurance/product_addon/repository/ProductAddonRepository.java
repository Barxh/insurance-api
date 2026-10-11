package com.fioneer.insurance.product_addon.repository;

import com.fioneer.insurance.product_addon.model.ProductAddon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ProductAddonRepository extends JpaRepository<ProductAddon, Long> {

    List<ProductAddon> findAllByProduct_IdAndAddon_IdIn(
            Long productId,
            Collection<Long> addonIds
    );
}
