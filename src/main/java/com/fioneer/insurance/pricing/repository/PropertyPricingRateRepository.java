package com.fioneer.insurance.pricing.repository;
import com.fioneer.insurance.pricing.model.PropertyPricingRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PropertyPricingRateRepository
        extends JpaRepository<PropertyPricingRate, Long> {

    Optional<PropertyPricingRate> findByInsurancePackage_Id(Long packageId);
}
