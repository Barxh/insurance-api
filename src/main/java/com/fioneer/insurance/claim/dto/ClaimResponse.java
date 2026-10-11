package com.fioneer.insurance.claim.dto;

import com.fioneer.insurance.claim.model.ClaimStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ClaimResponse(
        Long id,
        Long policyId,
        Person submittedBy,
        LocalDate incidentDate,
        String description,
        BigDecimal requestedAmount,
        String currency,
        ClaimStatus status,
        LocalDateTime submittedAt,
        Person reviewedBy,
        LocalDateTime reviewedAt,
        String decisionNote
) {
    public record Person(Long id, String username, String firstName, String lastName) {
    }
}
