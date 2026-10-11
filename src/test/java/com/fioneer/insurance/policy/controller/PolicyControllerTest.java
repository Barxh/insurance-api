package com.fioneer.insurance.policy.controller;

import com.fioneer.insurance.api.error.GlobalExceptionHandler;
import com.fioneer.insurance.policy.dto.PolicyResponse;
import com.fioneer.insurance.policy.dto.TravelPolicyRequest;
import com.fioneer.insurance.policy.service.PolicyPurchaseService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PolicyControllerTest {

    @Mock
    private PolicyPurchaseService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PolicyController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void listPoliciesPassesUsernameAndAuthoritiesToService() throws Exception {
        when(service.getPolicies("client1", Set.of("ROLE_CLIENT"))).thenReturn(List.of());

        mockMvc.perform(get("/api/policies").principal(client()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$" ).isArray())
                .andExpect(jsonPath("$.length()" ).value(0));

        verify(service).getPolicies("client1", Set.of("ROLE_CLIENT"));
    }

    @Test
    void getPolicyPassesIdUsernameAndAuthoritiesToService() throws Exception {
        when(service.getPolicy(23L, "agent1", Set.of("ROLE_AGENT"))).thenReturn(null);

        mockMvc.perform(get("/api/policies/23").principal(agent()))
                .andExpect(status().isOk());

        verify(service).getPolicy(23L, "agent1", Set.of("ROLE_AGENT"));
    }

    @Test
    void purchaseTravelReturnsCreatedAndUsesAuthenticatedUsername() throws Exception {
        when(service.purchaseTravel(eq("client1"), any(TravelPolicyRequest.class))).thenReturn(null);

        mockMvc.perform(post("/api/policies/travel")
                        .principal(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validTravelJson()))
                .andExpect(status().isCreated());

        verify(service).purchaseTravel(eq("client1"), any(TravelPolicyRequest.class));
    }

    @Test
    void purchaseTravelRejectsInvalidRequestBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/policies/travel")
                        .principal(client())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":0,\"region\":null,\"startDate\":null,\"endDate\":null,\"travelers\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.length()" ).value(org.hamcrest.Matchers.greaterThan(0)));

        org.mockito.Mockito.verifyNoInteractions(service);
    }

    private static UsernamePasswordAuthenticationToken client() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "client1", null, List.of(new SimpleGrantedAuthority("ROLE_CLIENT")));
    }

    private static UsernamePasswordAuthenticationToken agent() {
        return UsernamePasswordAuthenticationToken.authenticated(
                "agent1", null, List.of(new SimpleGrantedAuthority("ROLE_AGENT")));
    }

    private static String validTravelJson() {
        return """
                {"packageId":1,"addonIds":[],"region":"EUROPE","startDate":"2026-11-01","endDate":"2026-11-08","travelers":[{"firstName":"Nikola","lastName":"Nikolic","jmbg":"0101990712345"}]}
                """;
    }
}
