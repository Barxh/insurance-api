package com.fioneer.insurance.policy.repository;

import com.fioneer.insurance.policy.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
}
