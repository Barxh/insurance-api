package com.fioneer.insurance.pricing.dto;
import com.fioneer.insurance.policy.model.enums.VehicleType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
public record RoadsideRateRequest(
        @NotNull @Positive Long packageId,
        @NotNull VehicleType vehicleType,
        @NotNull @DecimalMin("0.00") BigDecimal annualPrice
) {
}
