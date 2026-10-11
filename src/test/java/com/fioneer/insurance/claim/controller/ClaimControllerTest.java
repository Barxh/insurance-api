package com.fioneer.insurance.claim.controller;

import com.fioneer.insurance.api.error.GlobalExceptionHandler;
import com.fioneer.insurance.claim.dto.CreateClaimRequest;
import com.fioneer.insurance.claim.dto.ReviewClaimRequest;
import com.fioneer.insurance.claim.service.ClaimService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClaimControllerTest {

    @Mock
    private ClaimService claimService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ClaimController(claimService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void clientClaimsPassAuthenticatedUserAndRoleToService() throws Exception {
        when(claimService.getClaims("client1", Set.of("ROLE_CLIENT"))).thenReturn(List.of());

        mockMvc.perform(get("/api/claims").principal(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()" ).value(0));

        verify(claimService).getClaims("client1", Set.of("ROLE_CLIENT"));
    }

    @Test
    void agentCanRequestClaimDetailsWithAgentAuthority() throws Exception {
        when(claimService.getClaim(42L, "agent1", Set.of("ROLE_AGENT"))).thenReturn(null);

        mockMvc.perform(get("/api/claims/42").principal(agent()))
                .andExpect(status().isOk());

        verify(claimService).getClaim(42L, "agent1", Set.of("ROLE_AGENT"));
    }

    @Test
    void clientCanSubmitClaimAndGetsCreated() throws Exception {
        when(claimService.createClaim(eq("client1"), any(CreateClaimRequest.class))).thenReturn(null);

        mockMvc.perform(post("/api/claims")
                        .principal(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyId\":15,\"incidentDate\":\"2026-10-01\",\"description\":\"Damage\",\"requestedAmount\":2500.00}"))
                .andExpect(status().isCreated());

        verify(claimService).createClaim(eq("client1"), any(CreateClaimRequest.class));
    }

    @Test
    void invalidClaimRequestReturnsValidationErrorsWithoutCallingService() throws Exception {
        mockMvc.perform(post("/api/claims")
                        .principal(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"policyId\":0,\"description\":\" \",\"requestedAmount\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.length()" ).value(org.hamcrest.Matchers.greaterThan(0)));

        verifyNoInteractions(claimService);
    }

    @Test
    void agentReviewPassesClaimIdReviewerAndDecision() throws Exception {
        when(claimService.reviewClaim(eq(42L), eq("agent1"), any(ReviewClaimRequest.class)))
                .thenReturn(null);

        mockMvc.perform(patch("/api/claims/42/review")
                        .principal(agent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"decisionNote\":\"Approved\"}"))
                .andExpect(status().isOk());

        verify(claimService).reviewClaim(eq(42L), eq("agent1"), any(ReviewClaimRequest.class));
    }

    @Test
    void invalidReviewRequestIsRejected() throws Exception {
        mockMvc.perform(patch("/api/claims/42/review")
                        .principal(agent())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":null,\"decisionNote\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()" ).value(org.hamcrest.Matchers.greaterThan(0)));

        verifyNoInteractions(claimService);
    }

    private static UsernamePasswordAuthenticationToken client() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "client1", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    private static UsernamePasswordAuthenticationToken agent() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "agent1", null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
    }
}
