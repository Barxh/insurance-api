package com.fioneer.insurance.claim.repository;

import com.fioneer.insurance.claim.model.Claim;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClaimRepository extends JpaRepository<Claim, Long> {

    List<Claim> findAllBySubmittedBy_UsernameOrderBySubmittedAtDesc(
            String username
    );

    List<Claim> findAllByOrderBySubmittedAtDesc();

    Optional<Claim> findByIdAndSubmittedBy_Username(Long id, String username);
}
