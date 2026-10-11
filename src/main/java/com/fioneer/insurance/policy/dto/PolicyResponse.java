package com.fioneer.insurance.policy.dto;

import com.fioneer.insurance.policy.model.enums.PolicyStatus;
import com.fioneer.insurance.policy.model.enums.TravelRegion;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import com.fioneer.insurance.policy.model.enums.VehicleType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PolicyResponse(
        Long id,
        Long productId,
        Long packageId,
        String packageName,
        BigDecimal packagePrice,
        String packageCurrency,
        BigDecimal totalPrice,
        String currency,
        PolicyStatus status,
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime createdAt,
        List<Addon> addons,
        List<Coverage> coverages,
        TravelRegion travelRegion,
        BigDecimal propertyAreaSquareMeters,
        BigDecimal propertyRatePerSquareMeter,
        VehicleType vehicleType,
        BigDecimal vehicleAnnualPrice,
        List<InsuredPerson> insuredPeople
) {
    public record InsuredPerson(String firstName, String lastName, TravelerAgeGroup ageGroup, BigDecimal dailyRate) {}
    public record Addon(
            Long id,
            String name,
            BigDecimal price,
            String currency
    ) {
    }

    public record Coverage(
            Long id,
            String name,
            BigDecimal limitAmount,
            String limitCurrency
    ) {
    }
}
