package com.fioneer.insurance.policy.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record PropertyPolicyRequest(
        @NotNull @Positive Long packageId,
        @Size(max = 50) List<@NotNull @Positive Long> addonIds,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull @DecimalMin("20.00") @Digits(integer = 8, fraction = 2)
        BigDecimal squareMeters
) {
}
