package com.fioneer.insurance.policy.repository;

import com.fioneer.insurance.policy.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findAllByUser_UsernameOrderByCreatedAtDesc(String username);

    List<Policy> findAllByOrderByCreatedAtDesc();

    Optional<Policy> findByIdAndUser_Username(Long id, String username);
}
