package com.fioneer.insurance.policy.dto;

import com.fioneer.insurance.policy.model.enums.TravelRegion;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record TravelPolicyRequest(
        @NotNull @Positive Long packageId,
        @Size(max = 50) List<@NotNull @Positive Long> addonIds,
        @NotNull TravelRegion region,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotEmpty @Size(max = 100) List<@Valid Traveler> travelers
) {
    public record Traveler(
            @NotBlank @Size(max = 50) String firstName,
            @NotBlank @Size(max = 50) String lastName,
            @NotBlank @Pattern(regexp = "\\d{13}") String jmbg
    ) {
    }
}
