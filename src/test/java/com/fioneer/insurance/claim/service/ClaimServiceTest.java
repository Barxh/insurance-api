package com.fioneer.insurance.claim.service;

import com.fioneer.insurance.claim.dto.ClaimResponse;
import com.fioneer.insurance.claim.dto.CreateClaimRequest;
import com.fioneer.insurance.claim.dto.ReviewClaimRequest;
import com.fioneer.insurance.claim.model.Claim;
import com.fioneer.insurance.claim.model.ClaimDecision;
import com.fioneer.insurance.claim.model.ClaimStatus;
import com.fioneer.insurance.claim.repository.ClaimRepository;
import com.fioneer.insurance.policy.model.Policy;
import com.fioneer.insurance.policy.repository.PolicyRepository;
import com.fioneer.insurance.user.model.Role;
import com.fioneer.insurance.user.model.User;
import com.fioneer.insurance.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaimServiceTest {

    private static final String CLIENT_USERNAME = "client1";
    private static final String AGENT_USERNAME = "agent1";
    private static final Long POLICY_ID = 10L;
    private static final Long CLAIM_ID = 25L;

    @Mock
    private ClaimRepository claims;

    @Mock
    private PolicyRepository policies;

    @Mock
    private UserRepository users;

    @InjectMocks
    private ClaimService service;

    private User client;
    private User agent;
    private Policy policy;
    private LocalDate coverageStart;
    private LocalDate coverageEnd;

    @BeforeEach
    void setUp() {
        client = user(1L, CLIENT_USERNAME, "ROLE_CLIENT");
        agent = user(2L, AGENT_USERNAME, "ROLE_AGENT");

        coverageStart = LocalDate.now().minusMonths(2);
        coverageEnd = LocalDate.now().plusMonths(10);

        policy = new Policy();
        policy.setId(POLICY_ID);
        policy.setUser(client);
        policy.setCurrency("RSD");
        policy.setStartDate(coverageStart);
        policy.setEndDate(coverageEnd);
    }

    @Test
    void createClaimStoresClientPolicyAndPolicyCurrency() {
        when(users.findByUsername(CLIENT_USERNAME))
                .thenReturn(Optional.of(client));
        when(policies.findByIdAndUser_Username(POLICY_ID, CLIENT_USERNAME))
                .thenReturn(Optional.of(policy));
        when(claims.save(any(Claim.class))).thenAnswer(invocation -> {
            Claim claim = invocation.getArgument(0);
            claim.setId(CLAIM_ID);
            return claim;
        });

        CreateClaimRequest request = createRequest(coverageStart, "  Water leak  ");
        ClaimResponse response = service.createClaim(CLIENT_USERNAME, request);

        assertEquals(CLAIM_ID, response.id());
        assertEquals(POLICY_ID, response.policyId());
        assertEquals(CLIENT_USERNAME, response.submittedBy().username());
        assertEquals(coverageStart, response.incidentDate());
        assertEquals("Water leak", response.description());
        assertEquals(new BigDecimal("1250.00"), response.requestedAmount());
        assertEquals("RSD", response.currency());
        assertEquals(ClaimStatus.PENDING, response.status());
        assertNotNull(response.submittedAt());
        assertEquals(null, response.reviewedBy());
        verify(claims).save(any(Claim.class));
    }

    @Test
    void createClaimRejectsPolicyThatDoesNotBelongToClient() {
        when(users.findByUsername(CLIENT_USERNAME))
                .thenReturn(Optional.of(client));
        when(policies.findByIdAndUser_Username(POLICY_ID, CLIENT_USERNAME))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createClaim(
                        CLIENT_USERNAME,
                        createRequest(LocalDate.now(), "Damage")
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(claims, never()).save(any(Claim.class));
    }

    @Test
    void createClaimRejectsIncidentBeforeCoveragePeriod() {
        when(users.findByUsername(CLIENT_USERNAME))
                .thenReturn(Optional.of(client));
        when(policies.findByIdAndUser_Username(POLICY_ID, CLIENT_USERNAME))
                .thenReturn(Optional.of(policy));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createClaim(
                        CLIENT_USERNAME,
                        createRequest(coverageStart.minusDays(1), "Damage")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(claims, never()).save(any(Claim.class));
    }

    @Test
    void createClaimRejectsIncidentAfterCoveragePeriod() {
        when(users.findByUsername(CLIENT_USERNAME))
                .thenReturn(Optional.of(client));
        when(policies.findByIdAndUser_Username(POLICY_ID, CLIENT_USERNAME))
                .thenReturn(Optional.of(policy));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createClaim(
                        CLIENT_USERNAME,
                        createRequest(coverageEnd.plusDays(1), "Damage")
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(claims, never()).save(any(Claim.class));
    }

    @Test
    void createClaimRejectsUserWithoutClientRole() {
        User agentWithoutClientRole = user(
                3L,
                "agent-only",
                "ROLE_AGENT"
        );
        when(users.findByUsername(agentWithoutClientRole.getUsername()))
                .thenReturn(Optional.of(agentWithoutClientRole));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.createClaim(
                        agentWithoutClientRole.getUsername(),
                        createRequest(LocalDate.now(), "Damage")
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(policies, never())
                .findByIdAndUser_Username(any(), any());
    }

    @Test
    void agentCanApprovePendingClaimAndDecisionIsRecorded() {
        Claim claim = pendingClaim();
        when(users.findByUsername(AGENT_USERNAME))
                .thenReturn(Optional.of(agent));
        when(claims.findById(CLAIM_ID)).thenReturn(Optional.of(claim));
        when(claims.save(claim)).thenReturn(claim);

        ClaimResponse response = service.reviewClaim(
                CLAIM_ID,
                AGENT_USERNAME,
                new ReviewClaimRequest(ClaimDecision.APPROVE, "  Covered  ")
        );

        assertEquals(ClaimStatus.APPROVED, response.status());
        assertEquals(AGENT_USERNAME, response.reviewedBy().username());
        assertNotNull(response.reviewedAt());
        assertEquals("Covered", response.decisionNote());
        verify(claims).save(claim);
    }

    @Test
    void agentCanRejectPendingClaim() {
        Claim claim = pendingClaim();
        when(users.findByUsername(AGENT_USERNAME))
                .thenReturn(Optional.of(agent));
        when(claims.findById(CLAIM_ID)).thenReturn(Optional.of(claim));
        when(claims.save(claim)).thenReturn(claim);

        ClaimResponse response = service.reviewClaim(
                CLAIM_ID,
                AGENT_USERNAME,
                new ReviewClaimRequest(
                        ClaimDecision.REJECT,
                        "Damage is outside policy coverage"
                )
        );

        assertEquals(ClaimStatus.REJECTED, response.status());
        assertEquals("Damage is outside policy coverage", response.decisionNote());
        assertEquals(AGENT_USERNAME, response.reviewedBy().username());
    }

    @Test
    void reviewRejectsClaimThatIsNoLongerPending() {
        Claim claim = pendingClaim();
        claim.setStatus(ClaimStatus.APPROVED);
        when(users.findByUsername(AGENT_USERNAME))
                .thenReturn(Optional.of(agent));
        when(claims.findById(CLAIM_ID)).thenReturn(Optional.of(claim));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.reviewClaim(
                        CLAIM_ID,
                        AGENT_USERNAME,
                        new ReviewClaimRequest(ClaimDecision.REJECT, "Duplicate")
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(claims, never()).save(any(Claim.class));
    }

    @Test
    void reviewerCannotReviewTheirOwnClaim() {
        Claim claim = pendingClaim();
        claim.setSubmittedBy(agent);
        when(users.findByUsername(AGENT_USERNAME))
                .thenReturn(Optional.of(agent));
        when(claims.findById(CLAIM_ID)).thenReturn(Optional.of(claim));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.reviewClaim(
                        CLAIM_ID,
                        AGENT_USERNAME,
                        new ReviewClaimRequest(ClaimDecision.APPROVE, "Approved")
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(claims, never()).save(any(Claim.class));
    }

    @Test
    void clientSeesOnlyTheirOwnClaims() {
        when(claims.findAllBySubmittedBy_UsernameOrderBySubmittedAtDesc(
                CLIENT_USERNAME
        )).thenReturn(List.of());

        List<ClaimResponse> response = service.getClaims(
                CLIENT_USERNAME,
                Set.of("ROLE_CLIENT")
        );

        assertTrue(response.isEmpty());
        verify(claims)
                .findAllBySubmittedBy_UsernameOrderBySubmittedAtDesc(CLIENT_USERNAME);
        verify(claims, never()).findAllByOrderBySubmittedAtDesc();
    }

    @Test
    void agentSeesAllClaims() {
        when(claims.findAllByOrderBySubmittedAtDesc()).thenReturn(List.of());

        List<ClaimResponse> response = service.getClaims(
                AGENT_USERNAME,
                Set.of("ROLE_AGENT")
        );

        assertTrue(response.isEmpty());
        verify(claims).findAllByOrderBySubmittedAtDesc();
        verify(claims, never())
                .findAllBySubmittedBy_UsernameOrderBySubmittedAtDesc(
                        AGENT_USERNAME
                );
    }

    @Test
    void clientCannotReadAnotherClientsClaim() {
        when(claims.findByIdAndSubmittedBy_Username(CLAIM_ID, CLIENT_USERNAME))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.getClaim(
                        CLAIM_ID,
                        CLIENT_USERNAME,
                        Set.of("ROLE_CLIENT")
                )
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    private User user(Long id, String username, String roleName) {
        Role role = new Role();
        role.setName(roleName);

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setFirstName("First");
        user.setLastName("Last");
        user.setRoles(new HashSet<>(Set.of(role)));
        return user;
    }

    private CreateClaimRequest createRequest(
            LocalDate incidentDate,
            String description
    ) {
        return new CreateClaimRequest(
                POLICY_ID,
                incidentDate,
                description,
                new BigDecimal("1250.00")
        );
    }

    private Claim pendingClaim() {
        Claim claim = new Claim();
        claim.setId(CLAIM_ID);
        claim.setPolicy(policy);
        claim.setSubmittedBy(client);
        claim.setIncidentDate(LocalDate.now());
        claim.setDescription("Damage description");
        claim.setRequestedAmount(new BigDecimal("1250.00"));
        claim.setCurrency("RSD");
        claim.setStatus(ClaimStatus.PENDING);
        return claim;
    }
}
