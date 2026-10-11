package com.fioneer.insurance.insurance_package.repository;

import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsurancePackageRepository extends JpaRepository<InsurancePackage, Long> {
}
