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
import com.fioneer.insurance.user.model.User;
import com.fioneer.insurance.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Service
public class ClaimService {

    private final ClaimRepository claims;
    private final PolicyRepository policies;
    private final UserRepository users;

    public ClaimService(
            ClaimRepository claims,
            PolicyRepository policies,
            UserRepository users
    ) {
        this.claims = claims;
        this.policies = policies;
        this.users = users;
    }

    @Transactional
    public ClaimResponse createClaim(String username, CreateClaimRequest request) {
        User user = findUser(username);
        requireRole(user, "ROLE_CLIENT");

        Policy policy = policies.findByIdAndUser_Username(
                        request.policyId(),
                        username
                )
                .orElseThrow(() -> claimNotFound(null));

        if (request.incidentDate().isBefore(policy.getStartDate())
                || request.incidentDate().isAfter(policy.getEndDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Incident date must be within the policy coverage period"
            );
        }

        Claim claim = new Claim();
        claim.setPolicy(policy);
        claim.setSubmittedBy(user);
        claim.setIncidentDate(request.incidentDate());
        claim.setDescription(request.description().trim());
        claim.setRequestedAmount(request.requestedAmount());
        claim.setCurrency(policy.getCurrency());
        claim.setStatus(ClaimStatus.PENDING);
        claim.setSubmittedAt(LocalDateTime.now());

        return toResponse(claims.save(claim));
    }

    @Transactional(readOnly = true)
    public List<ClaimResponse> getClaims(
            String username,
            Set<String> authorities
    ) {
        List<Claim> visibleClaims;
        if (authorities.contains("ROLE_AGENT")
                || authorities.contains("ROLE_ADMIN")) {
            visibleClaims = claims.findAllByOrderBySubmittedAtDesc();
        } else if (authorities.contains("ROLE_CLIENT")) {
            visibleClaims = claims
                    .findAllBySubmittedBy_UsernameOrderBySubmittedAtDesc(username);
        } else {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You are not allowed to view claims"
            );
        }

        return visibleClaims.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ClaimResponse getClaim(
            Long claimId,
            String username,
            Set<String> authorities
    ) {
        boolean canViewAllClaims = authorities.contains("ROLE_AGENT")
                || authorities.contains("ROLE_ADMIN");

        Claim claim = canViewAllClaims
                ? claims.findById(claimId).orElseThrow(() -> claimNotFound(claimId))
                : claims.findByIdAndSubmittedBy_Username(claimId, username)
                        .orElseThrow(() -> claimNotFound(claimId));

        return toResponse(claim);
    }

    @Transactional
    public ClaimResponse reviewClaim(
            Long claimId,
            String reviewerUsername,
            ReviewClaimRequest request
    ) {
        User reviewer = findUser(reviewerUsername);
        requireRole(reviewer, "ROLE_AGENT");

        Claim claim = claims.findById(claimId)
                .orElseThrow(() -> claimNotFound(claimId));
        if (claim.getStatus() != ClaimStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only pending claims can be reviewed"
            );
        }
        if (claim.getSubmittedBy().getId().equals(reviewer.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "A claim cannot be reviewed by the user who submitted it"
            );
        }

        claim.setStatus(request.decision() == ClaimDecision.APPROVE
                ? ClaimStatus.APPROVED
                : ClaimStatus.REJECTED);
        claim.setReviewedBy(reviewer);
        claim.setReviewedAt(LocalDateTime.now());
        claim.setDecisionNote(request.decisionNote().trim());

        return toResponse(claims.save(claim));
    }

    private User findUser(String username) {
        return users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Authenticated user was not found"
                ));
    }

    private void requireRole(User user, String roleName) {
        boolean hasRole = user.getRoles().stream()
                .anyMatch(role -> roleName.equals(role.getName()));
        if (!hasRole) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Your role is not allowed to perform this action"
            );
        }
    }

    private ClaimResponse toResponse(Claim claim) {
        return new ClaimResponse(
                claim.getId(),
                claim.getPolicy().getId(),
                person(claim.getSubmittedBy()),
                claim.getIncidentDate(),
                claim.getDescription(),
                claim.getRequestedAmount(),
                claim.getCurrency(),
                claim.getStatus(),
                claim.getSubmittedAt(),
                claim.getReviewedBy() == null
                        ? null
                        : person(claim.getReviewedBy()),
                claim.getReviewedAt(),
                claim.getDecisionNote()
        );
    }

    private ClaimResponse.Person person(User user) {
        return new ClaimResponse.Person(
                user.getId(),
                user.getUsername(),
                user.getFirstName(),
                user.getLastName()
        );
    }

    private ResponseStatusException claimNotFound(Long claimId) {
        String message = claimId == null
                ? "Policy was not found or does not belong to the authenticated client"
                : "Claim " + claimId + " was not found";
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
