package com.fioneer.insurance.claim.dto;

import com.fioneer.insurance.claim.model.ClaimDecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReviewClaimRequest(
        @NotNull ClaimDecision decision,
        @NotBlank @Size(max = 1000) String decisionNote
) {
}
