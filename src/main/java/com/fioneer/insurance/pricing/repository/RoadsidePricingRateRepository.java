package com.fioneer.insurance.pricing.repository;
import com.fioneer.insurance.pricing.model.RoadsidePricingRate;
import com.fioneer.insurance.policy.model.enums.VehicleType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoadsidePricingRateRepository
        extends JpaRepository<RoadsidePricingRate, Long> {

    Optional<RoadsidePricingRate> findByInsurancePackage_IdAndVehicleType(
            Long packageId,
            VehicleType vehicleType
    );
}
