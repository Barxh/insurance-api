package com.fioneer.insurance.policy.repository;

import com.fioneer.insurance.policy.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    List<Policy> findAllByUser_UsernameOrderByCreatedAtDesc(String username);

    List<Policy> findAllByOrderByCreatedAtDesc();
}
