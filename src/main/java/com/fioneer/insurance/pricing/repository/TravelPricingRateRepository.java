package com.fioneer.insurance.pricing.repository;
import com.fioneer.insurance.pricing.model.TravelPricingRate;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import com.fioneer.insurance.policy.model.enums.TravelRegion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TravelPricingRateRepository
        extends JpaRepository<TravelPricingRate, Long> {

    Optional<TravelPricingRate> findByInsurancePackage_IdAndRegionAndAgeGroup(
            Long packageId,
            TravelRegion region,
            TravelerAgeGroup ageGroup
    );
}
