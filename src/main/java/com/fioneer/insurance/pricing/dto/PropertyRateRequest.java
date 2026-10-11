package com.fioneer.insurance.pricing.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
public record PropertyRateRequest(
        @NotNull @Positive Long packageId,
        @NotNull @DecimalMin("0.00") BigDecimal pricePerSquareMeter
) {
}
