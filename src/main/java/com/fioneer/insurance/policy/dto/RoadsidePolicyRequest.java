package com.fioneer.insurance.policy.dto;

import com.fioneer.insurance.policy.model.enums.VehicleType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record RoadsidePolicyRequest(
        @NotNull @Positive Long packageId,
        @Size(max = 50) List<@NotNull @Positive Long> addonIds,
        @NotNull @FutureOrPresent LocalDate startDate,
        @NotNull VehicleType vehicleType
) {
}
