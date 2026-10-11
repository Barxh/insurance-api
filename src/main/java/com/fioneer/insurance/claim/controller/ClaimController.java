package com.fioneer.insurance.claim.controller;

import com.fioneer.insurance.claim.dto.ClaimResponse;
import com.fioneer.insurance.claim.dto.CreateClaimRequest;
import com.fioneer.insurance.claim.dto.ReviewClaimRequest;
import com.fioneer.insurance.claim.service.ClaimService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/claims")
public class ClaimController {

    private final ClaimService claimService;

    public ClaimController(ClaimService claimService) {
        this.claimService = claimService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public ClaimResponse createClaim(
            @Valid @RequestBody CreateClaimRequest request,
            Authentication authentication
    ) {
        return claimService.createClaim(authentication.getName(), request);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENT', 'ROLE_AGENT', 'ROLE_ADMIN')")
    public List<ClaimResponse> getClaims(Authentication authentication) {
        return claimService.getClaims(
                authentication.getName(),
                authorities(authentication)
        );
    }

    @GetMapping("/{claimId}")
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENT', 'ROLE_AGENT', 'ROLE_ADMIN')")
    public ClaimResponse getClaim(
            @PathVariable Long claimId,
            Authentication authentication
    ) {
        return claimService.getClaim(
                claimId,
                authentication.getName(),
                authorities(authentication)
        );
    }

    @PatchMapping("/{claimId}/review")
    @PreAuthorize("hasAuthority('ROLE_AGENT')")
    public ClaimResponse reviewClaim(
            @PathVariable Long claimId,
            @Valid @RequestBody ReviewClaimRequest request,
            Authentication authentication
    ) {
        return claimService.reviewClaim(
                claimId,
                authentication.getName(),
                request
        );
    }

    private Set<String> authorities(Authentication authentication) {
        return authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());
    }
}
