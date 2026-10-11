package com.fioneer.insurance.policy.controller;

import com.fioneer.insurance.policy.dto.PolicyResponse;
import com.fioneer.insurance.policy.dto.PropertyPolicyRequest;
import com.fioneer.insurance.policy.dto.RoadsidePolicyRequest;
import com.fioneer.insurance.policy.dto.TravelPolicyRequest;
import com.fioneer.insurance.policy.service.PolicyPurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyPurchaseService service;

    public PolicyController(PolicyPurchaseService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('ROLE_CLIENT', 'ROLE_AGENT', 'ROLE_ADMIN')")
    public List<PolicyResponse> getPolicies(Authentication authentication) {
        Set<String> authorities = authentication.getAuthorities()
                .stream()
                .map(authority -> authority.getAuthority())
                .collect(Collectors.toSet());

        return service.getPolicies(authentication.getName(), authorities);
    }

    @PostMapping("/travel")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public PolicyResponse travel(
            @Valid @RequestBody TravelPolicyRequest request,
            Authentication authentication
    ) {
        return service.purchaseTravel(authentication.getName(), request);
    }

    @PostMapping("/property")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public PolicyResponse property(
            @Valid @RequestBody PropertyPolicyRequest request,
            Authentication authentication
    ) {
        return service.purchaseProperty(authentication.getName(), request);
    }

    @PostMapping("/roadside")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('ROLE_CLIENT')")
    public PolicyResponse roadside(
            @Valid @RequestBody RoadsidePolicyRequest request,
            Authentication authentication
    ) {
        return service.purchaseRoadside(authentication.getName(), request);
    }
}
