package com.fioneer.insurance.insurance_product.repository;

import com.fioneer.insurance.insurance_product.model.InsuranceProduct;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceProductRepository extends JpaRepository<InsuranceProduct, Long> {
}
