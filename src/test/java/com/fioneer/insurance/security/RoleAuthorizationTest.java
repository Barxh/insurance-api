package com.fioneer.insurance.security;

import com.fioneer.insurance.claim.controller.ClaimController;
import com.fioneer.insurance.claim.dto.CreateClaimRequest;
import com.fioneer.insurance.claim.dto.ReviewClaimRequest;
import com.fioneer.insurance.claim.model.ClaimDecision;
import com.fioneer.insurance.claim.service.ClaimService;
import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.insurance_package.repository.InsurancePackageRepository;
import com.fioneer.insurance.insurance_product.model.InsuranceProduct;
import com.fioneer.insurance.insurance_product.model.InsuranceType;
import com.fioneer.insurance.policy.controller.PolicyController;
import com.fioneer.insurance.policy.dto.TravelPolicyRequest;
import com.fioneer.insurance.policy.model.enums.TravelRegion;
import com.fioneer.insurance.policy.service.PolicyPurchaseService;
import com.fioneer.insurance.pricing.controller.PricingAdminController;
import com.fioneer.insurance.pricing.dto.TravelRateRequest;
import com.fioneer.insurance.pricing.repository.PropertyPricingRateRepository;
import com.fioneer.insurance.pricing.repository.RoadsidePricingRateRepository;
import com.fioneer.insurance.pricing.repository.TravelPricingRateRepository;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(RoleAuthorizationTest.TestConfig.class)
class RoleAuthorizationTest {

    @Autowired private PolicyController policyController;
    @Autowired private PolicyPurchaseService policyService;
    @Autowired private ClaimController claimController;
    @Autowired private ClaimService claimService;
    @Autowired private PricingAdminController pricingController;
    @Autowired private InsurancePackageRepository packages;
    @Autowired private TravelPricingRateRepository travelRates;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clientCanPurchasePolicyAndAgentCannot() {
        TravelPolicyRequest request = new TravelPolicyRequest(1L, List.of(), TravelRegion.EUROPE,
                LocalDate.now().plusDays(1), LocalDate.now().plusDays(8),
                List.of(new TravelPolicyRequest.Traveler("Nikola", "Nikolic", "0101990712345")));

        authenticate("client1", "ROLE_CLIENT");
        policyController.travel(request, authentication("client1", "ROLE_CLIENT"));
        verify(policyService).purchaseTravel("client1", request);
        clearInvocations(policyService);

        authenticate("agent1", "ROLE_AGENT");
        assertThrows(AccessDeniedException.class,
                () -> policyController.travel(request, authentication("agent1", "ROLE_AGENT")));
        verify(policyService, never()).purchaseTravel(any(), any());
    }

    @Test
    void clientCanCreateClaimButCannotReviewIt() {
        CreateClaimRequest create = new CreateClaimRequest(7L, LocalDate.now(), "Damage", new BigDecimal("100.00"));

        authenticate("client1", "ROLE_CLIENT");
        claimController.createClaim(create, authentication("client1", "ROLE_CLIENT"));
        verify(claimService).createClaim("client1", create);

        authenticate("client1", "ROLE_CLIENT");
        assertThrows(AccessDeniedException.class, () -> claimController.reviewClaim(
                7L, new ReviewClaimRequest(ClaimDecision.APPROVE, "Reviewed"),
                authentication("client1", "ROLE_CLIENT")));
        verify(claimService, never()).reviewClaim(any(), any(), any());
    }

    @Test
    void agentCanReviewClaim() {
        ReviewClaimRequest review = new ReviewClaimRequest(ClaimDecision.APPROVE, "Reviewed");
        authenticate("agent1", "ROLE_AGENT");

        claimController.reviewClaim(7L, review, authentication("agent1", "ROLE_AGENT"));

        verify(claimService).reviewClaim(7L, "agent1", review);
    }

    @Test
    void onlyAdminCanSetPricing() {
        TravelRateRequest request = new TravelRateRequest(
                4L, TravelRegion.EUROPE, TravelerAgeGroup.AGE_19_TO_70, new BigDecimal("2.50"));
        authenticate("client1", "ROLE_CLIENT");

        assertThrows(AccessDeniedException.class, () -> pricingController.setTravelRate(request));

        authenticate("admin1", "ROLE_ADMIN");
        InsuranceProduct product = InsuranceProduct.builder()
                .insuranceType(InsuranceType.TRAVEL_HEALTH).build();
        InsurancePackage insurancePackage = InsurancePackage.builder()
                .id(4L).product(product).currency("RSD").active(true).build();
        when(packages.findById(4L)).thenReturn(Optional.of(insurancePackage));
        when(travelRates.findByInsurancePackage_IdAndRegionAndAgeGroup(
                4L, TravelRegion.EUROPE, TravelerAgeGroup.AGE_19_TO_70)).thenReturn(Optional.empty());

        pricingController.setTravelRate(request);

        verify(travelRates).save(any());
    }

    private static void authenticate(String username, String role) {
        SecurityContextHolder.getContext().setAuthentication(authentication(username, role));
    }

    private static UsernamePasswordAuthenticationToken authentication(String username, String role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                username, null, List.of(new SimpleGrantedAuthority(role)));
    }

    @Configuration
    @EnableMethodSecurity
    static class TestConfig {
        @Bean PolicyPurchaseService policyService() { return mock(PolicyPurchaseService.class); }
        @Bean ClaimService claimService() { return mock(ClaimService.class); }
        @Bean InsurancePackageRepository packages() { return mock(InsurancePackageRepository.class); }
        @Bean TravelPricingRateRepository travelRates() { return mock(TravelPricingRateRepository.class); }
        @Bean PropertyPricingRateRepository propertyRates() { return mock(PropertyPricingRateRepository.class); }
        @Bean RoadsidePricingRateRepository roadsideRates() { return mock(RoadsidePricingRateRepository.class); }
        @Bean PolicyController policyController(PolicyPurchaseService service) { return new PolicyController(service); }
        @Bean ClaimController claimController(ClaimService service) { return new ClaimController(service); }
        @Bean PricingAdminController pricingController(
                InsurancePackageRepository packages,
                TravelPricingRateRepository travelRates,
                PropertyPricingRateRepository propertyRates,
                RoadsidePricingRateRepository roadsideRates
        ) {
            return new PricingAdminController(packages, travelRates, propertyRates, roadsideRates);
        }
    }
}
