package com.fioneer.insurance.pricing.controller;

import com.fioneer.insurance.api.error.GlobalExceptionHandler;
import com.fioneer.insurance.insurance_package.model.InsurancePackage;
import com.fioneer.insurance.insurance_package.repository.InsurancePackageRepository;
import com.fioneer.insurance.insurance_product.model.InsuranceProduct;
import com.fioneer.insurance.insurance_product.model.InsuranceType;
import com.fioneer.insurance.policy.model.enums.TravelRegion;
import com.fioneer.insurance.policy.model.enums.TravelerAgeGroup;
import com.fioneer.insurance.policy.model.enums.VehicleType;
import com.fioneer.insurance.pricing.model.PropertyPricingRate;
import com.fioneer.insurance.pricing.model.RoadsidePricingRate;
import com.fioneer.insurance.pricing.model.TravelPricingRate;
import com.fioneer.insurance.pricing.repository.PropertyPricingRateRepository;
import com.fioneer.insurance.pricing.repository.RoadsidePricingRateRepository;
import com.fioneer.insurance.pricing.repository.TravelPricingRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PricingAdminControllerTest {

    private static final long PACKAGE_ID = 12L;

    @Mock private InsurancePackageRepository packages;
    @Mock private TravelPricingRateRepository travelRates;
    @Mock private PropertyPricingRateRepository propertyRates;
    @Mock private RoadsidePricingRateRepository roadsideRates;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new PricingAdminController(
                        packages, travelRates, propertyRates, roadsideRates))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void savesTravelRateWithPackageCurrency() throws Exception {
        when(packages.findById(PACKAGE_ID)).thenReturn(Optional.of(packageFor(InsuranceType.TRAVEL_HEALTH)));
        when(travelRates.findByInsurancePackage_IdAndRegionAndAgeGroup(
                PACKAGE_ID, TravelRegion.EUROPE, TravelerAgeGroup.AGE_19_TO_70)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/pricing/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":12,\"region\":\"EUROPE\",\"ageGroup\":\"AGE_19_TO_70\",\"dailyRate\":3.25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.packageId").value(PACKAGE_ID))
                .andExpect(jsonPath("$.currency").value("RSD"))
                .andExpect(jsonPath("$.dailyRate").value(3.25));

        ArgumentCaptor<TravelPricingRate> captor = ArgumentCaptor.forClass(TravelPricingRate.class);
        verify(travelRates).save(captor.capture());
        assertEquals(new BigDecimal("3.25"), captor.getValue().getDailyRate());
        assertEquals("RSD", captor.getValue().getCurrency());
    }

    @Test
    void savesPropertyRate() throws Exception {
        when(packages.findById(PACKAGE_ID)).thenReturn(Optional.of(packageFor(InsuranceType.PROPERTY)));
        when(propertyRates.findByInsurancePackage_Id(PACKAGE_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/pricing/property")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":12,\"pricePerSquareMeter\":125.50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.pricePerSquareMeter").value(125.50));

        ArgumentCaptor<PropertyPricingRate> captor = ArgumentCaptor.forClass(PropertyPricingRate.class);
        verify(propertyRates).save(captor.capture());
        assertEquals(new BigDecimal("125.50"), captor.getValue().getPricePerSquareMeter());
        assertEquals("RSD", captor.getValue().getCurrency());
    }

    @Test
    void savesRoadsideRateForVehicleType() throws Exception {
        when(packages.findById(PACKAGE_ID)).thenReturn(Optional.of(packageFor(InsuranceType.ROADSIDE_ASSISTANCE)));
        when(roadsideRates.findByInsurancePackage_IdAndVehicleType(PACKAGE_ID, VehicleType.CARGO))
                .thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/pricing/roadside")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":12,\"vehicleType\":\"CARGO\",\"annualPrice\":7200.00}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.vehicleType").value("CARGO"))
                .andExpect(jsonPath("$.annualPrice").value(7200.00));

        ArgumentCaptor<RoadsidePricingRate> captor = ArgumentCaptor.forClass(RoadsidePricingRate.class);
        verify(roadsideRates).save(captor.capture());
        assertEquals(VehicleType.CARGO, captor.getValue().getVehicleType());
        assertEquals("RSD", captor.getValue().getCurrency());
    }

    @Test
    void returnsNotFoundWhenPackageDoesNotExist() throws Exception {
        when(packages.findById(PACKAGE_ID)).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/admin/pricing/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":12,\"region\":\"EUROPE\",\"ageGroup\":\"AGE_19_TO_70\",\"dailyRate\":3.25}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Package not found"));

        verifyNoInteractions(travelRates);
    }

    @Test
    void rejectsInactivePackage() throws Exception {
        InsurancePackage inactive = packageFor(InsuranceType.PROPERTY);
        inactive.setActive(false);
        when(packages.findById(PACKAGE_ID)).thenReturn(Optional.of(inactive));

        mockMvc.perform(post("/api/admin/pricing/property")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":12,\"pricePerSquareMeter\":100}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Package is inactive"));

        verifyNoInteractions(propertyRates);
    }

    @Test
    void rejectsPackageOfDifferentInsuranceType() throws Exception {
        when(packages.findById(PACKAGE_ID)).thenReturn(Optional.of(packageFor(InsuranceType.PROPERTY)));

        mockMvc.perform(post("/api/admin/pricing/travel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":12,\"region\":\"EUROPE\",\"ageGroup\":\"AGE_19_TO_70\",\"dailyRate\":3.25}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Package belongs to a different insurance type"));

        verifyNoInteractions(travelRates);
    }

    @Test
    void validatesPricingRequestBeforeLookingUpPackage() throws Exception {
        mockMvc.perform(post("/api/admin/pricing/roadside")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"packageId\":0,\"vehicleType\":null,\"annualPrice\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.length()")
                        .value(org.hamcrest.Matchers.greaterThan(0)));

        verifyNoInteractions(packages, roadsideRates);
    }

    private static InsurancePackage packageFor(InsuranceType type) {
        InsuranceProduct product = InsuranceProduct.builder().insuranceType(type).build();
        return InsurancePackage.builder()
                .id(PACKAGE_ID)
                .product(product)
                .currency("RSD")
                .active(true)
                .build();
    }
}
