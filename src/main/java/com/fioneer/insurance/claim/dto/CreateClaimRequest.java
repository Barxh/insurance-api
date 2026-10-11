package com.fioneer.insurance.claim.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateClaimRequest(
        @NotNull @Positive Long policyId,
        @NotNull LocalDate incidentDate,
        @NotBlank @Size(max = 2000) String description,
        @NotNull @DecimalMin(value = "0.01")
        @Digits(integer = 10, fraction = 2) BigDecimal requestedAmount
) {
}
