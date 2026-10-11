package com.fioneer.insurance.policy.service;

import com.fioneer.insurance.insurance_addon.model.InsuranceAddon;
import com.fioneer.insurance.insurance_addon.repository.InsuranceAddonRepository;
import com.fioneer.insurance.insurance_coverage.model.InsuranceCoverage;
import com.fioneer.insurance.insurance_coverage.repository.InsuranceCoverageRepository;
import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.insurance_package.repository.InsurancePackageRepository;
import com.fioneer.insurance.insurance_product.model.InsuranceType;
import com.fioneer.insurance.policy.dto.PolicyResponse;
import com.fioneer.insurance.policy.dto.PropertyPolicyRequest;
import com.fioneer.insurance.policy.dto.RoadsidePolicyRequest;
import com.fioneer.insurance.policy.dto.TravelPolicyRequest;
import com.fioneer.insurance.policy.model.Policy;
import com.fioneer.insurance.policy.model.PolicyAddon;
import com.fioneer.insurance.policy.model.PolicyCoverage;
import com.fioneer.insurance.policy.model.PolicyInsuredPerson;
import com.fioneer.insurance.policy.model.PropertyPolicyDetails;
import com.fioneer.insurance.policy.model.RoadsidePolicyDetails;
import com.fioneer.insurance.policy.model.TravelPolicyDetails;
import com.fioneer.insurance.policy.model.enums.PolicyStatus;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import com.fioneer.insurance.policy.repository.PolicyRepository;
import com.fioneer.insurance.pricing.model.PropertyPricingRate;
import com.fioneer.insurance.pricing.model.RoadsidePricingRate;
import com.fioneer.insurance.pricing.model.TravelPricingRate;
import com.fioneer.insurance.pricing.repository.PropertyPricingRateRepository;
import com.fioneer.insurance.pricing.repository.RoadsidePricingRateRepository;
import com.fioneer.insurance.pricing.repository.TravelPricingRateRepository;
import com.fioneer.insurance.product_addon.repository.ProductAddonRepository;
import com.fioneer.insurance.user.model.User;
import com.fioneer.insurance.user.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PolicyPurchaseService {

    private final UserRepository users;
    private final InsurancePackageRepository packages;
    private final InsuranceAddonRepository addons;
    private final InsuranceCoverageRepository coverages;
    private final ProductAddonRepository productAddons;
    private final PolicyRepository policies;
    private final TravelPricingRateRepository travelRates;
    private final PropertyPricingRateRepository propertyRates;
    private final RoadsidePricingRateRepository roadsideRates;

    public PolicyPurchaseService(
            UserRepository users,
            InsurancePackageRepository packages,
            InsuranceAddonRepository addons,
            InsuranceCoverageRepository coverages,
            ProductAddonRepository productAddons,
            PolicyRepository policies,
            TravelPricingRateRepository travelRates,
            PropertyPricingRateRepository propertyRates,
            RoadsidePricingRateRepository roadsideRates
    ) {
        this.users = users;
        this.packages = packages;
        this.addons = addons;
        this.coverages = coverages;
        this.productAddons = productAddons;
        this.policies = policies;
        this.travelRates = travelRates;
        this.propertyRates = propertyRates;
        this.roadsideRates = roadsideRates;
    }

    @Transactional
    public PolicyResponse purchaseTravel(String username, TravelPolicyRequest request) {
        User user = findClient(username);
        InsurancePackage insurancePackage = findPackage(
                request.packageId(),
                InsuranceType.TRAVEL_HEALTH
        );
        validateDates(request.startDate(), request.endDate());

        if (request.endDate().isBefore(request.startDate())) {
            throw badRequest("endDate must be on or after startDate");
        }

        validateTravelers(request.travelers());
        List<InsuranceAddon> selectedAddons = findAddons(
                insurancePackage,
                request.addonIds()
        );

        long travelDays = ChronoUnit.DAYS.between(
                request.startDate(),
                request.endDate()
        ) + 1;

        Policy policy = createBasePolicy(
                user,
                insurancePackage,
                request.startDate(),
                request.endDate()
        );
        TravelPolicyDetails travelDetails = new TravelPolicyDetails();
        travelDetails.setPolicy(policy);
        travelDetails.setRegion(request.region());
        policy.setTravelDetails(travelDetails);

        BigDecimal premium = insurancePackage.getPrice();
        for (TravelPolicyRequest.Traveler traveler : request.travelers()) {
            LocalDate birthDate = birthDate(traveler.jmbg());
            TravelerAgeGroup ageGroup = ageGroup(birthDate, request.startDate());

            TravelPricingRate rate = travelRates
                    .findByInsurancePackage_IdAndRegionAndAgeGroup(
                            insurancePackage.getId(),
                            request.region(),
                            ageGroup
                    )
                    .orElseThrow(() -> badRequest(
                            "Travel pricing is not configured for this region and age group"
                    ));

            validateCurrency(insurancePackage.getCurrency(), rate.getCurrency());
            premium = premium.add(
                    rate.getDailyRate().multiply(BigDecimal.valueOf(travelDays))
            );

            PolicyInsuredPerson insuredPerson = new PolicyInsuredPerson();
            insuredPerson.setTravelDetails(travelDetails);
            insuredPerson.setFirstName(traveler.firstName());
            insuredPerson.setLastName(traveler.lastName());
            insuredPerson.setJmbg(traveler.jmbg());
            insuredPerson.setBirthDate(birthDate);
            insuredPerson.setAgeGroup(ageGroup);
            insuredPerson.setDailyRate(rate.getDailyRate());
            travelDetails.getInsuredPeople().add(insuredPerson);
        }

        return savePolicy(policy, selectedAddons, premium);
    }

    @Transactional
    public PolicyResponse purchaseProperty(
            String username,
            PropertyPolicyRequest request
    ) {
        User user = findClient(username);
        InsurancePackage insurancePackage = findPackage(
                request.packageId(),
                InsuranceType.PROPERTY
        );
        validateDates(request.startDate(), request.startDate());

        if (request.squareMeters().compareTo(new BigDecimal("20")) < 0) {
            throw badRequest("Property area must be at least 20 square meters");
        }

        PropertyPricingRate rate = propertyRates
                .findByInsurancePackage_Id(insurancePackage.getId())
                .orElseThrow(() -> badRequest(
                        "Property pricing is not configured for this package"
                ));
        validateCurrency(insurancePackage.getCurrency(), rate.getCurrency());

        LocalDate endDate = request.startDate().plusYears(1).minusDays(1);
        Policy policy = createBasePolicy(
                user,
                insurancePackage,
                request.startDate(),
                endDate
        );
        PropertyPolicyDetails propertyDetails = new PropertyPolicyDetails();
        propertyDetails.setPolicy(policy);
        propertyDetails.setAreaSquareMeters(request.squareMeters());
        propertyDetails.setRatePerSquareMeter(rate.getPricePerSquareMeter());
        policy.setPropertyDetails(propertyDetails);

        BigDecimal premium = insurancePackage.getPrice().add(
                rate.getPricePerSquareMeter().multiply(request.squareMeters())
        );
        List<InsuranceAddon> selectedAddons = findAddons(
                insurancePackage,
                request.addonIds()
        );

        return savePolicy(policy, selectedAddons, premium);
    }

    @Transactional
    public PolicyResponse purchaseRoadside(
            String username,
            RoadsidePolicyRequest request
    ) {
        User user = findClient(username);
        InsurancePackage insurancePackage = findPackage(
                request.packageId(),
                InsuranceType.ROADSIDE_ASSISTANCE
        );
        validateDates(request.startDate(), request.startDate());

        RoadsidePricingRate rate = roadsideRates
                .findByInsurancePackage_IdAndVehicleType(
                        insurancePackage.getId(),
                        request.vehicleType()
                )
                .orElseThrow(() -> badRequest(
                        "Roadside pricing is not configured for this package and vehicle type"
                ));
        validateCurrency(insurancePackage.getCurrency(), rate.getCurrency());

        LocalDate endDate = request.startDate().plusYears(1).minusDays(1);
        Policy policy = createBasePolicy(
                user,
                insurancePackage,
                request.startDate(),
                endDate
        );
        RoadsidePolicyDetails roadsideDetails = new RoadsidePolicyDetails();
        roadsideDetails.setPolicy(policy);
        roadsideDetails.setVehicleType(request.vehicleType());
        roadsideDetails.setAnnualPrice(rate.getAnnualPrice());
        policy.setRoadsideDetails(roadsideDetails);

        List<InsuranceAddon> selectedAddons = findAddons(
                insurancePackage,
                request.addonIds()
        );
        return savePolicy(policy, selectedAddons, rate.getAnnualPrice());
    }

    private PolicyResponse savePolicy(
            Policy policy,
            List<InsuranceAddon> selectedAddons,
            BigDecimal calculatedPremium
    ) {
        BigDecimal totalPrice = calculatedPremium;

        for (InsuranceAddon addon : selectedAddons) {
            PolicyAddon policyAddon = new PolicyAddon();
            policyAddon.setPolicy(policy);
            policyAddon.setAddon(addon);
            policyAddon.setAddonName(addon.getName());
            policyAddon.setAddonPrice(addon.getPrice());
            policyAddon.setAddonCurrency(addon.getCurrency());
            policy.getAddons().add(policyAddon);
            totalPrice = totalPrice.add(addon.getPrice());
        }

        List<InsuranceCoverage> packageCoverages = coverages
                .findAllByInsurancePackage_Id(
                        policy.getInsurancePackage().getId()
                );
        for (InsuranceCoverage coverage : packageCoverages) {
            PolicyCoverage policyCoverage = new PolicyCoverage();
            policyCoverage.setPolicy(policy);
            policyCoverage.setCoverage(coverage);
            policyCoverage.setCoverageName(coverage.getName());
            policyCoverage.setLimitAmount(coverage.getLimitAmount());
            policyCoverage.setLimitCurrency(coverage.getLimitCurrency());
            policy.getCoverages().add(policyCoverage);
        }

        policy.setTotalPrice(totalPrice);
        return toResponse(policies.save(policy));
    }

    private Policy createBasePolicy(
            User user,
            InsurancePackage insurancePackage,
            LocalDate startDate,
            LocalDate endDate
    ) {
        Policy policy = new Policy();
        policy.setUser(user);
        policy.setProduct(insurancePackage.getProduct());
        policy.setInsurancePackage(insurancePackage);
        policy.setPackageName(insurancePackage.getName());
        policy.setPackagePrice(insurancePackage.getPrice());
        policy.setPackageCurrency(insurancePackage.getCurrency());
        policy.setCurrency(insurancePackage.getCurrency());
        policy.setStatus(PolicyStatus.ACTIVE);
        policy.setStartDate(startDate);
        policy.setEndDate(endDate);
        policy.setCreatedAt(LocalDateTime.now());
        policy.setAddons(new ArrayList<>());
        policy.setCoverages(new ArrayList<>());
        return policy;
    }

    private List<InsuranceAddon> findAddons(
            InsurancePackage insurancePackage,
            List<Long> addonIds
    ) {
        if (addonIds == null || addonIds.isEmpty()) {
            return List.of();
        }
        if (new HashSet<>(addonIds).size() != addonIds.size()) {
            throw badRequest("addonIds must not contain duplicates");
        }

        Map<Long, InsuranceAddon> addonsById = addons.findAllById(addonIds)
                .stream()
                .collect(Collectors.toMap(InsuranceAddon::getId, addon -> addon));
        if (addonsById.size() != addonIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "One or more add-ons were not found"
            );
        }

        Set<Long> allowedAddonIds = productAddons
                .findAllByProduct_IdAndAddon_IdIn(
                        insurancePackage.getProduct().getId(),
                        addonIds
                )
                .stream()
                .map(productAddon -> productAddon.getAddon().getId())
                .collect(Collectors.toSet());
        if (!allowedAddonIds.containsAll(addonIds)) {
            throw badRequest(
                    "One or more add-ons are not available for the selected product"
            );
        }

        List<InsuranceAddon> selectedAddons = addonIds.stream()
                .map(addonsById::get)
                .toList();
        for (InsuranceAddon addon : selectedAddons) {
            if (!addon.isActive()) {
                throw badRequest("Inactive add-ons cannot be added");
            }
            validateCurrency(insurancePackage.getCurrency(), addon.getCurrency());
        }
        return selectedAddons;
    }

    private InsurancePackage findPackage(Long packageId, InsuranceType type) {
        InsurancePackage insurancePackage = packages.findById(packageId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Package was not found"
                ));

        if (!insurancePackage.isActive()
                || !insurancePackage.getProduct().isActive()) {
            throw badRequest("Selected package or product is inactive");
        }
        if (insurancePackage.getProduct().getInsuranceType() != type) {
            throw badRequest(
                    "Package does not belong to the requested insurance type"
            );
        }
        return insurancePackage;
    }

    private User findClient(String username) {
        User user = users.findByUsername(username)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Authenticated user was not found"
                ));

        boolean isClient = user.getRoles().stream()
                .anyMatch(role -> "ROLE_CLIENT".equals(role.getName()));
        if (!isClient) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only clients can purchase policies"
            );
        }
        return user;
    }

    private void validateDates(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || startDate.isBefore(LocalDate.now())) {
            throw badRequest("Policy start date cannot be in the past");
        }
        if (endDate == null) {
            throw badRequest("Policy end date is required");
        }
    }

    private void validateTravelers(List<TravelPolicyRequest.Traveler> travelers) {
        Set<String> jmbgs = new HashSet<>();
        for (TravelPolicyRequest.Traveler traveler : travelers) {
            validateJmbg(traveler.jmbg());
            if (!jmbgs.add(traveler.jmbg())) {
                throw badRequest("Traveler JMBGs must be unique");
            }
        }
    }

    private LocalDate birthDate(String jmbg) {
        int day = Integer.parseInt(jmbg.substring(0, 2));
        int month = Integer.parseInt(jmbg.substring(2, 4));
        int yearDigits = Integer.parseInt(jmbg.substring(4, 7));
        int year = (yearDigits >= 900 ? 1900 : 2000) + yearDigits;

        try {
            return LocalDate.of(year, month, day);
        } catch (DateTimeException exception) {
            throw badRequest("Invalid date of birth in JMBG");
        }
    }

    private void validateJmbg(String jmbg) {
        LocalDate dateOfBirth = birthDate(jmbg);
        if (dateOfBirth.isAfter(LocalDate.now())) {
            throw badRequest("JMBG date of birth cannot be in the future");
        }

        int[] digits = jmbg.chars()
                .map(character -> character - '0')
                .toArray();
        int sum = 7 * (digits[0] + digits[6])
                + 6 * (digits[1] + digits[7])
                + 5 * (digits[2] + digits[8])
                + 4 * (digits[3] + digits[9])
                + 3 * (digits[4] + digits[10])
                + 2 * (digits[5] + digits[11]);

        int checksum = 11 - sum % 11;
        if (checksum >= 10) {
            checksum = 0;
        }
        if (checksum != digits[12]) {
            throw badRequest("Invalid JMBG checksum");
        }
    }

    private TravelerAgeGroup ageGroup(LocalDate birthDate, LocalDate asOfDate) {
        int age = Period.between(birthDate, asOfDate).getYears();
        if (age < 0) {
            throw badRequest("Traveler cannot be born after policy start");
        }
        if (age < 4) {
            return TravelerAgeGroup.UNDER_4;
        }
        if (age <= 18) {
            return TravelerAgeGroup.AGE_4_TO_18;
        }
        if (age <= 70) {
            return TravelerAgeGroup.AGE_19_TO_70;
        }
        return TravelerAgeGroup.OVER_70;
    }

    private void validateCurrency(String expectedCurrency, String actualCurrency) {
        if (!Objects.equals(expectedCurrency, actualCurrency)) {
            throw badRequest("All rates and add-ons must use package currency");
        }
    }

    private PolicyResponse toResponse(Policy policy) {
        List<PolicyResponse.Addon> policyAddons = policy.getAddons().stream()
                .map(addon -> new PolicyResponse.Addon(
                        addon.getAddon().getId(),
                        addon.getAddonName(),
                        addon.getAddonPrice(),
                        addon.getAddonCurrency()
                ))
                .toList();

        List<PolicyResponse.Coverage> policyCoverages = policy.getCoverages()
                .stream()
                .map(coverage -> new PolicyResponse.Coverage(
                        coverage.getCoverage().getId(),
                        coverage.getCoverageName(),
                        coverage.getLimitAmount(),
                        coverage.getLimitCurrency()
                ))
                .toList();

        List<PolicyResponse.InsuredPerson> insuredPeople = policy.getTravelDetails() == null
                ? List.of()
                : policy.getTravelDetails().getInsuredPeople()
                .stream()
                .map(person -> new PolicyResponse.InsuredPerson(
                        person.getFirstName(),
                        person.getLastName(),
                        person.getAgeGroup(),
                        person.getDailyRate()
                ))
                .toList();

        return new PolicyResponse(
                policy.getId(),
                policy.getProduct().getId(),
                policy.getInsurancePackage().getId(),
                policy.getPackageName(),
                policy.getPackagePrice(),
                policy.getPackageCurrency(),
                policy.getTotalPrice(),
                policy.getCurrency(),
                policy.getStatus(),
                policy.getStartDate(),
                policy.getEndDate(),
                policy.getCreatedAt(),
                policyAddons,
                policyCoverages,
                policy.getTravelDetails() == null
                        ? null
                        : policy.getTravelDetails().getRegion(),
                policy.getPropertyDetails() == null
                        ? null
                        : policy.getPropertyDetails().getAreaSquareMeters(),
                policy.getPropertyDetails() == null
                        ? null
                        : policy.getPropertyDetails().getRatePerSquareMeter(),
                policy.getRoadsideDetails() == null
                        ? null
                        : policy.getRoadsideDetails().getVehicleType(),
                policy.getRoadsideDetails() == null
                        ? null
                        : policy.getRoadsideDetails().getAnnualPrice(),
                insuredPeople
        );
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
