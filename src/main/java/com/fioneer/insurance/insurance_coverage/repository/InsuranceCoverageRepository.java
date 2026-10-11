package com.fioneer.insurance.insurance_coverage.repository;

import com.fioneer.insurance.insurance_coverage.model.InsuranceCoverage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InsuranceCoverageRepository extends JpaRepository<InsuranceCoverage, Long> {

    List<InsuranceCoverage> findAllByInsurancePackage_Id(Long packageId);
}
