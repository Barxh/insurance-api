package com.fioneer.insurance.pricing.dto;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import com.fioneer.insurance.policy.model.enums.TravelRegion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
public record TravelRateRequest(
        @NotNull @Positive Long packageId,
        @NotNull TravelRegion region,
        @NotNull TravelerAgeGroup ageGroup,
        @NotNull @DecimalMin("0.00") BigDecimal dailyRate
) {
}
