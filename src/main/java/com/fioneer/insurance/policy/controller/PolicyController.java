package com.fioneer.insurance.policy.controller;
import com.fioneer.insurance.policy.dto.*;
import com.fioneer.insurance.policy.service.PolicyPurchaseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/policies")
public class PolicyController {
 private final PolicyPurchaseService service;

 public PolicyController(PolicyPurchaseService service){
  this.service=service;
 }

 @PostMapping("/travel")
 @ResponseStatus(HttpStatus.CREATED)
 @PreAuthorize("hasAuthority('ROLE_CLIENT')")
 public PolicyResponse travel(@Valid @RequestBody TravelPolicyRequest request,Authentication auth){
  return service.purchaseTravel(auth.getName(),request);
 }

 @PostMapping("/property")
 @ResponseStatus(HttpStatus.CREATED)
 @PreAuthorize("hasAuthority('ROLE_CLIENT')")
 public PolicyResponse property(@Valid @RequestBody PropertyPolicyRequest request,Authentication auth){
  return service.purchaseProperty(auth.getName(),request);
 }

 @PostMapping("/roadside")
 @ResponseStatus(HttpStatus.CREATED)
 @PreAuthorize("hasAuthority('ROLE_CLIENT')")
 public PolicyResponse roadside(@Valid @RequestBody RoadsidePolicyRequest request,Authentication auth){
  return service.purchaseRoadside(auth.getName(),request);
 }
}
