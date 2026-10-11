package com.fioneer.insurance.pricing.controller;

import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.insurance_package.repository.InsurancePackageRepository;
import com.fioneer.insurance.insurance_product.model.InsuranceType;
import com.fioneer.insurance.pricing.dto.PropertyRateRequest;
import com.fioneer.insurance.pricing.dto.RoadsideRateRequest;
import com.fioneer.insurance.pricing.dto.TravelRateRequest;
import com.fioneer.insurance.pricing.model.PropertyPricingRate;
import com.fioneer.insurance.pricing.model.RoadsidePricingRate;
import com.fioneer.insurance.pricing.model.TravelPricingRate;
import com.fioneer.insurance.pricing.repository.PropertyPricingRateRepository;
import com.fioneer.insurance.pricing.repository.RoadsidePricingRateRepository;
import com.fioneer.insurance.pricing.repository.TravelPricingRateRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/pricing")
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class PricingAdminController {

    private final InsurancePackageRepository packages;
    private final TravelPricingRateRepository travelRates;
    private final PropertyPricingRateRepository propertyRates;
    private final RoadsidePricingRateRepository roadsideRates;

    public PricingAdminController(
            InsurancePackageRepository packages,
            TravelPricingRateRepository travelRates,
            PropertyPricingRateRepository propertyRates,
            RoadsidePricingRateRepository roadsideRates
    ) {
        this.packages = packages;
        this.travelRates = travelRates;
        this.propertyRates = propertyRates;
        this.roadsideRates = roadsideRates;
    }

    @PostMapping("/travel")
    @Transactional
    public Map<String, Object> setTravelRate(
            @Valid @RequestBody TravelRateRequest request
    ) {
        InsurancePackage insurancePackage = findPackage(
                request.packageId(),
                InsuranceType.TRAVEL_HEALTH
        );

        TravelPricingRate rate = travelRates
                .findByInsurancePackage_IdAndRegionAndAgeGroup(
                        insurancePackage.getId(),
                        request.region(),
                        request.ageGroup()
                )
                .orElseGet(TravelPricingRate::new);
        rate.setInsurancePackage(insurancePackage);
        rate.setRegion(request.region());
        rate.setAgeGroup(request.ageGroup());
        rate.setDailyRate(request.dailyRate());
        rate.setCurrency(insurancePackage.getCurrency());
        travelRates.save(rate);

        return Map.of(
                "saved", true,
                "packageId", insurancePackage.getId(),
                "region", request.region(),
                "ageGroup", request.ageGroup(),
                "dailyRate", request.dailyRate(),
                "currency", insurancePackage.getCurrency()
        );
    }

    @PostMapping("/property")
    @Transactional
    public Map<String, Object> setPropertyRate(
            @Valid @RequestBody PropertyRateRequest request
    ) {
        InsurancePackage insurancePackage = findPackage(
                request.packageId(),
                InsuranceType.PROPERTY
        );

        PropertyPricingRate rate = propertyRates
                .findByInsurancePackage_Id(insurancePackage.getId())
                .orElseGet(PropertyPricingRate::new);
        rate.setInsurancePackage(insurancePackage);
        rate.setPricePerSquareMeter(request.pricePerSquareMeter());
        rate.setCurrency(insurancePackage.getCurrency());
        propertyRates.save(rate);

        return Map.of(
                "saved", true,
                "packageId", insurancePackage.getId(),
                "pricePerSquareMeter", request.pricePerSquareMeter(),
                "currency", insurancePackage.getCurrency()
        );
    }

    @PostMapping("/roadside")
    @Transactional
    public Map<String, Object> setRoadsideRate(
            @Valid @RequestBody RoadsideRateRequest request
    ) {
        InsurancePackage insurancePackage = findPackage(
                request.packageId(),
                InsuranceType.ROADSIDE_ASSISTANCE
        );

        RoadsidePricingRate rate = roadsideRates
                .findByInsurancePackage_IdAndVehicleType(
                        insurancePackage.getId(),
                        request.vehicleType()
                )
                .orElseGet(RoadsidePricingRate::new);
        rate.setInsurancePackage(insurancePackage);
        rate.setVehicleType(request.vehicleType());
        rate.setAnnualPrice(request.annualPrice());
        rate.setCurrency(insurancePackage.getCurrency());
        roadsideRates.save(rate);

        return Map.of(
                "saved", true,
                "packageId", insurancePackage.getId(),
                "vehicleType", request.vehicleType(),
                "annualPrice", request.annualPrice(),
                "currency", insurancePackage.getCurrency()
        );
    }

    private InsurancePackage findPackage(Long packageId, InsuranceType type) {
        InsurancePackage insurancePackage = packages.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Package not found"
                ));

        if (!insurancePackage.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Package is inactive"
            );
        }

        InsuranceType currentType = insurancePackage.getProduct()
                .getInsuranceType();
        if (currentType != null && currentType != type) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Package belongs to a different insurance type"
            );
        }

        insurancePackage.getProduct().setInsuranceType(type);
        return insurancePackage;
    }
}
