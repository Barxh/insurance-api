package com.fioneer.insurance.policy.service;

import com.fioneer.insurance.insurance_addon.repository.InsuranceAddonRepository;
import com.fioneer.insurance.insurance_addon.model.InsuranceAddon;
import com.fioneer.insurance.insurance_coverage.repository.InsuranceCoverageRepository;
import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.insurance_package.repository.InsurancePackageRepository;
import com.fioneer.insurance.insurance_product.model.InsuranceProduct;
import com.fioneer.insurance.insurance_product.model.InsuranceType;
import com.fioneer.insurance.policy.dto.PolicyResponse;
import com.fioneer.insurance.policy.dto.PropertyPolicyRequest;
import com.fioneer.insurance.policy.dto.RoadsidePolicyRequest;
import com.fioneer.insurance.policy.dto.TravelPolicyRequest;
import com.fioneer.insurance.policy.model.Policy;
import com.fioneer.insurance.product_addon.model.ProductAddon;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import com.fioneer.insurance.policy.model.enums.TravelRegion;
import com.fioneer.insurance.policy.model.enums.VehicleType;
import com.fioneer.insurance.policy.repository.PolicyRepository;
import com.fioneer.insurance.pricing.model.PropertyPricingRate;
import com.fioneer.insurance.pricing.model.RoadsidePricingRate;
import com.fioneer.insurance.pricing.model.TravelPricingRate;
import com.fioneer.insurance.pricing.repository.PropertyPricingRateRepository;
import com.fioneer.insurance.pricing.repository.RoadsidePricingRateRepository;
import com.fioneer.insurance.pricing.repository.TravelPricingRateRepository;
import com.fioneer.insurance.product_addon.repository.ProductAddonRepository;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyPurchaseServiceTest {

    private static final String USERNAME = "client1";
    private static final Long PACKAGE_ID = 20L;
    private static final String CURRENCY = "RSD";

    @Mock
    private UserRepository users;

    @Mock
    private InsurancePackageRepository packages;

    @Mock
    private InsuranceAddonRepository addons;

    @Mock
    private InsuranceCoverageRepository coverages;

    @Mock
    private ProductAddonRepository productAddons;

    @Mock
    private PolicyRepository policies;

    @Mock
    private TravelPricingRateRepository travelRates;

    @Mock
    private PropertyPricingRateRepository propertyRates;

    @Mock
    private RoadsidePricingRateRepository roadsideRates;

    @InjectMocks
    private PolicyPurchaseService service;

    private User client;
    private InsuranceProduct product;
    private InsurancePackage insurancePackage;

    @BeforeEach
    void setUp() {
        Role clientRole = new Role();
        clientRole.setName("ROLE_CLIENT");

        client = new User();
        client.setId(5L);
        client.setUsername(USERNAME);
        client.setFirstName("Mila");
        client.setLastName("Petrovic");
        client.setRoles(new HashSet<>(Set.of(clientRole)));

        product = InsuranceProduct.builder()
                .id(10L)
                .name("Insurance product")
                .active(true)
                .build();

        insurancePackage = InsurancePackage.builder()
                .id(PACKAGE_ID)
                .product(product)
                .name("Gold")
                .price(new BigDecimal("100.00"))
                .currency(CURRENCY)
                .active(true)
                .build();

        lenient().when(users.findByUsername(USERNAME))
                .thenReturn(Optional.of(client));
        lenient().when(packages.findById(PACKAGE_ID))
                .thenReturn(Optional.of(insurancePackage));
        lenient().when(coverages.findAllByInsurancePackage_Id(PACKAGE_ID))
                .thenReturn(List.of());
        lenient().when(policies.save(any(Policy.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void purchaseTravelCalculatesPackageAndPerTravelerDailyRates() {
        product.setInsuranceType(InsuranceType.TRAVEL_HEALTH);
        LocalDate startDate = LocalDate.now().plusDays(2);
        LocalDate endDate = startDate.plusDays(2);
        String adultJmbg = jmbg(startDate.minusYears(35), 1);
        String seniorJmbg = jmbg(startDate.minusYears(72), 2);

        when(travelRates.findByInsurancePackage_IdAndRegionAndAgeGroup(
                PACKAGE_ID,
                TravelRegion.EUROPE,
                TravelerAgeGroup.AGE_19_TO_70
        )).thenReturn(Optional.of(travelRate(
                TravelerAgeGroup.AGE_19_TO_70,
                "10.00"
        )));
        when(travelRates.findByInsurancePackage_IdAndRegionAndAgeGroup(
                PACKAGE_ID,
                TravelRegion.EUROPE,
                TravelerAgeGroup.OVER_70
        )).thenReturn(Optional.of(travelRate(
                TravelerAgeGroup.OVER_70,
                "30.00"
        )));

        TravelPolicyRequest request = new TravelPolicyRequest(
                PACKAGE_ID,
                List.of(),
                TravelRegion.EUROPE,
                startDate,
                endDate,
                List.of(
                        new TravelPolicyRequest.Traveler(
                                "Adult",
                                "Traveler",
                                adultJmbg
                        ),
                        new TravelPolicyRequest.Traveler(
                                "Senior",
                                "Traveler",
                                seniorJmbg
                        )
                )
        );

        PolicyResponse response = service.purchaseTravel(USERNAME, request);

        assertEquals(new BigDecimal("220.00"), response.totalPrice());
        assertEquals(TravelRegion.EUROPE, response.travelRegion());
        assertEquals(2, response.insuredPeople().size());
        assertEquals(
                TravelerAgeGroup.AGE_19_TO_70,
                response.insuredPeople().get(0).ageGroup()
        );
        assertEquals(
                TravelerAgeGroup.OVER_70,
                response.insuredPeople().get(1).ageGroup()
        );
        assertEquals(endDate, response.endDate());
    }

    @Test
    void purchasePropertyCalculatesPackageAndAreaRateAndSetsOneYearPeriod() {
        product.setInsuranceType(InsuranceType.PROPERTY);
        PropertyPricingRate rate = new PropertyPricingRate();
        rate.setPricePerSquareMeter(new BigDecimal("12.50"));
        rate.setCurrency(CURRENCY);
        when(propertyRates.findByInsurancePackage_Id(PACKAGE_ID))
                .thenReturn(Optional.of(rate));
        InsuranceAddon addon = InsuranceAddon.builder()
                .id(7L)
                .name("Extra cover")
                .price(new BigDecimal("50.00"))
                .currency(CURRENCY)
                .active(true)
                .build();
        ProductAddon productAddon = new ProductAddon();
        productAddon.setProduct(product);
        productAddon.setAddon(addon);
        when(addons.findAllById(List.of(7L))).thenReturn(List.of(addon));
        when(productAddons.findAllByProduct_IdAndAddon_IdIn(
                product.getId(),
                List.of(7L)
        )).thenReturn(List.of(productAddon));

        LocalDate startDate = LocalDate.now().plusDays(2);
        PropertyPolicyRequest request = new PropertyPolicyRequest(
                PACKAGE_ID,
                List.of(7L),
                startDate,
                new BigDecimal("80.00")
        );

        PolicyResponse response = service.purchaseProperty(USERNAME, request);

        assertEquals(new BigDecimal("1150.0000"), response.totalPrice());
        assertEquals(1, response.addons().size());
        assertEquals(new BigDecimal("80.00"), response.propertyAreaSquareMeters());
        assertEquals(
                startDate.plusYears(1).minusDays(1),
                response.endDate()
        );
    }

    @Test
    void purchaseRoadsideUsesVehicleSpecificAnnualRate() {
        product.setInsuranceType(InsuranceType.ROADSIDE_ASSISTANCE);
        RoadsidePricingRate rate = new RoadsidePricingRate();
        rate.setVehicleType(VehicleType.CARGO);
        rate.setAnnualPrice(new BigDecimal("850.00"));
        rate.setCurrency(CURRENCY);
        when(roadsideRates.findByInsurancePackage_IdAndVehicleType(
                PACKAGE_ID,
                VehicleType.CARGO
        )).thenReturn(Optional.of(rate));

        LocalDate startDate = LocalDate.now().plusDays(2);
        RoadsidePolicyRequest request = new RoadsidePolicyRequest(
                PACKAGE_ID,
                List.of(),
                startDate,
                VehicleType.CARGO
        );

        PolicyResponse response = service.purchaseRoadside(USERNAME, request);

        assertEquals(new BigDecimal("850.00"), response.totalPrice());
        assertEquals(VehicleType.CARGO, response.vehicleType());
        assertEquals(new BigDecimal("850.00"), response.vehicleAnnualPrice());
        assertEquals(startDate.plusYears(1).minusDays(1), response.endDate());
    }

    @Test
    void purchasePropertyRejectsAreaBelowMinimum() {
        product.setInsuranceType(InsuranceType.PROPERTY);
        LocalDate startDate = LocalDate.now().plusDays(2);
        PropertyPolicyRequest request = new PropertyPolicyRequest(
                PACKAGE_ID,
                List.of(),
                startDate,
                new BigDecimal("19.99")
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.purchaseProperty(USERNAME, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(propertyRates, never()).findByInsurancePackage_Id(PACKAGE_ID);
        verify(policies, never()).save(any(Policy.class));
    }

    @Test
    void purchaseRejectsPackageOfDifferentInsuranceType() {
        product.setInsuranceType(InsuranceType.PROPERTY);
        RoadsidePolicyRequest request = new RoadsidePolicyRequest(
                PACKAGE_ID,
                List.of(),
                LocalDate.now().plusDays(2),
                VehicleType.PASSENGER
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.purchaseRoadside(USERNAME, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(roadsideRates, never())
                .findByInsurancePackage_IdAndVehicleType(
                        PACKAGE_ID,
                        VehicleType.PASSENGER
                );
        verify(policies, never()).save(any(Policy.class));
    }

    @Test
    void purchaseTravelRejectsJmbgWithInvalidChecksum() {
        product.setInsuranceType(InsuranceType.TRAVEL_HEALTH);
        LocalDate startDate = LocalDate.now().plusDays(2);
        String validJmbg = jmbg(startDate.minusYears(30), 3);
        String invalidJmbg = validJmbg.substring(0, 12)
                + (validJmbg.charAt(12) == '9' ? "0" : "9");
        TravelPolicyRequest request = new TravelPolicyRequest(
                PACKAGE_ID,
                List.of(),
                TravelRegion.WORLDWIDE,
                startDate,
                startDate,
                List.of(new TravelPolicyRequest.Traveler(
                        "Invalid",
                        "Checksum",
                        invalidJmbg
                ))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.purchaseTravel(USERNAME, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("checksum"));
        verify(policies, never()).save(any(Policy.class));
    }

    @Test
    void purchaseIsForbiddenForNonClientUser() {
        Role agentRole = new Role();
        agentRole.setName("ROLE_AGENT");
        client.setRoles(new HashSet<>(Set.of(agentRole)));
        product.setInsuranceType(InsuranceType.PROPERTY);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.purchaseProperty(
                        USERNAME,
                        new PropertyPolicyRequest(
                                PACKAGE_ID,
                                List.of(),
                                LocalDate.now().plusDays(2),
                                new BigDecimal("30.00")
                        )
                )
        );

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(packages, never()).findById(PACKAGE_ID);
    }

    @Test
    void purchaseTravelRejectsMissingAgeRegionTariff() {
        product.setInsuranceType(InsuranceType.TRAVEL_HEALTH);
        LocalDate startDate = LocalDate.now().plusDays(2);
        String travelerJmbg = jmbg(startDate.minusYears(30), 4);
        when(travelRates.findByInsurancePackage_IdAndRegionAndAgeGroup(
                PACKAGE_ID,
                TravelRegion.EUROPE,
                TravelerAgeGroup.AGE_19_TO_70
        )).thenReturn(Optional.empty());

        TravelPolicyRequest request = new TravelPolicyRequest(
                PACKAGE_ID,
                List.of(),
                TravelRegion.EUROPE,
                startDate,
                startDate,
                List.of(new TravelPolicyRequest.Traveler(
                        "Adult",
                        "Traveler",
                        travelerJmbg
                ))
        );

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.purchaseTravel(USERNAME, request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Travel pricing is not configured"));
        verify(policies, never()).save(any(Policy.class));
    }

    @Test
    void getPoliciesReturnsOnlyTheClientsPoliciesForClientRole() {
        when(policies.findAllByUser_UsernameOrderByCreatedAtDesc(USERNAME))
                .thenReturn(List.of());

        List<PolicyResponse> response = service.getPolicies(
                USERNAME,
                Set.of("ROLE_CLIENT")
        );

        assertTrue(response.isEmpty());
        verify(policies).findAllByUser_UsernameOrderByCreatedAtDesc(USERNAME);
        verify(policies, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void getPoliciesReturnsAllPoliciesForAgentRole() {
        when(policies.findAllByOrderByCreatedAtDesc()).thenReturn(List.of());

        List<PolicyResponse> response = service.getPolicies(
                "agent1",
                Set.of("ROLE_AGENT")
        );

        assertTrue(response.isEmpty());
        verify(policies).findAllByOrderByCreatedAtDesc();
        verify(policies, never())
                .findAllByUser_UsernameOrderByCreatedAtDesc("agent1");
    }

    private TravelPricingRate travelRate(
            TravelerAgeGroup ageGroup,
            String dailyRate
    ) {
        TravelPricingRate rate = new TravelPricingRate();
        rate.setRegion(TravelRegion.EUROPE);
        rate.setAgeGroup(ageGroup);
        rate.setDailyRate(new BigDecimal(dailyRate));
        rate.setCurrency(CURRENCY);
        return rate;
    }

    private String jmbg(LocalDate birthDate, int serial) {
        int encodedYear = birthDate.getYear() < 2000
                ? birthDate.getYear() - 1900 + 900
                : birthDate.getYear() - 2000;
        String firstTwelveDigits = String.format(
                "%02d%02d%03d%05d",
                birthDate.getDayOfMonth(),
                birthDate.getMonthValue(),
                encodedYear,
                serial
        );
        int[] digits = firstTwelveDigits.chars()
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
        return firstTwelveDigits + checksum;
    }
}
