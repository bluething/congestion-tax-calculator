package io.github.bluething.congestion.calculator.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CongestionTaxService Tests")
class CongestionTaxServiceTest {

    @Mock
    private TaxCalculator taxCalculator;

    @Mock
    private TaxRulesConfig taxRulesConfig;

    @Mock
    private ValidationService validationService;

    @Mock
    private VehicleFactory vehicleFactory;

    @InjectMocks
    private CongestionTaxService congestionTaxService;

    private Vehicle mockCar;
    private Vehicle mockMotorcycle;

    @BeforeEach
    void setUp() {
        mockCar = mock(Vehicle.class);
        mockMotorcycle = mock(Vehicle.class);
    }

    @Nested
    @DisplayName("Happy Path Tests")
    class HappyPathTests {

        @Test
        @DisplayName("Should calculate tax for single day with regular vehicle")
        void shouldCalculateTaxForSingleDayRegularVehicle() {
            // Given
            LocalDateTime passage1 = LocalDateTime.of(2013, 2, 8, 6, 23);
            LocalDateTime passage2 = LocalDateTime.of(2013, 2, 8, 15, 27);

            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Arrays.asList(passage1, passage2));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(eq(mockCar), any(LocalDateTime[].class))).thenReturn(31);
            when(taxCalculator.getTollFee(passage1, mockCar)).thenReturn(8);
            when(taxCalculator.getTollFee(passage2, mockCar)).thenReturn(13);
            when(taxCalculator.isTollFreeDate(any(LocalDateTime.class))).thenReturn(false);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getVehicleType()).isEqualTo("Car");
            assertThat(response.getTotalTax()).isEqualTo(31);
            assertThat(response.isTollFreeVehicle()).isFalse();
            assertThat(response.getDailySummaries()).hasSize(1);
            assertThat(response.getPassageCalculations()).hasSize(2);

            verify(validationService).validateServiceRequest(request);
            verify(vehicleFactory).createVehicle("Car");
            verify(taxCalculator).getTax(eq(mockCar), any(LocalDateTime[].class));
        }

        @Test
        @DisplayName("Should calculate tax for multiple days")
        void shouldCalculateTaxForMultipleDays() {
            // Given
            LocalDateTime day1Passage = LocalDateTime.of(2013, 2, 7, 6, 23);
            LocalDateTime day2Passage1 = LocalDateTime.of(2013, 2, 8, 6, 27);
            LocalDateTime day2Passage2 = LocalDateTime.of(2013, 2, 8, 15, 29);

            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Arrays.asList(day1Passage, day2Passage1, day2Passage2));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);

            // Mock daily calculations
            when(taxCalculator.getTax(eq(mockCar), argThat(dates -> dates.length == 1)))
                    .thenReturn(8); // Day 1
            when(taxCalculator.getTax(eq(mockCar), argThat(dates -> dates.length == 2)))
                    .thenReturn(31); // Day 2

            when(taxCalculator.getTollFee(any(LocalDateTime.class), eq(mockCar)))
                    .thenReturn(8, 8, 13); // Individual fees
            when(taxCalculator.isTollFreeDate(any(LocalDateTime.class))).thenReturn(false);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(39); // 8 + 31
            assertThat(response.getDailySummaries()).hasSize(2);
            assertThat(response.getPassageCalculations()).hasSize(3);

            DailyTaxSummary day1Summary = response.getDailySummaries().stream()
                    .filter(s -> s.getDate().equals(LocalDate.of(2013, 2, 7)))
                    .findFirst().orElse(null);
            assertThat(day1Summary).isNotNull();
            assertThat(day1Summary.getDailyTax()).isEqualTo(8);
            assertThat(day1Summary.getPassageCount()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Toll-Free Vehicle Tests")
    class TollFreeVehicleTests {

        @ParameterizedTest
        @ValueSource(strings = {"Motorcycle", "Emergency", "Diplomat", "Military", "Foreign", "Tractor"})
        @DisplayName("Should return zero tax for toll-free vehicles")
        void shouldReturnZeroTaxForTollFreeVehicles(String vehicleType) {
            // Given
            LocalDateTime passage = LocalDateTime.of(2013, 2, 8, 7, 30);
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest(vehicleType, Collections.singletonList(passage));

            Vehicle mockVehicle = mock(Vehicle.class);
            when(mockVehicle.getVehicleType()).thenReturn(vehicleType);
            when(vehicleFactory.createVehicle(vehicleType)).thenReturn(mockVehicle);
            when(taxRulesConfig.isTollFreeVehicle(vehicleType)).thenReturn(true);
            when(taxCalculator.getTax(eq(mockVehicle), any(LocalDateTime[].class))).thenReturn(0);
            when(taxCalculator.getTollFee(passage, mockVehicle)).thenReturn(0);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(0);
            assertThat(response.isTollFreeVehicle()).isTrue();
            assertThat(response.getDailySummaries()).hasSize(1);
            assertThat(response.getDailySummaries().getFirst().getReason()).isEqualTo("Toll-free vehicle type");
        }

        @Test
        @DisplayName("Should handle toll-free vehicle with multiple passages")
        void shouldHandleTollFreeVehicleWithMultiplePassages() {
            // Given
            LocalDateTime passage1 = LocalDateTime.of(2013, 2, 8, 7, 30);
            LocalDateTime passage2 = LocalDateTime.of(2013, 2, 8, 15, 30);
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Motorcycle", Arrays.asList(passage1, passage2));

            when(mockMotorcycle.getVehicleType()).thenReturn("Motorcycle");
            when(vehicleFactory.createVehicle("Motorcycle")).thenReturn(mockMotorcycle);
            when(taxRulesConfig.isTollFreeVehicle("Motorcycle")).thenReturn(true);
            when(taxCalculator.getTax(eq(mockMotorcycle), any(LocalDateTime[].class))).thenReturn(0);
            when(taxCalculator.getTollFee(any(LocalDateTime.class), eq(mockMotorcycle))).thenReturn(0);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(0);
            assertThat(response.getPassageCalculations()).hasSize(2);
            response.getPassageCalculations().forEach(calc -> assertThat(calc.getReason()).contains("Toll-free vehicle type: Motorcycle"));
        }
    }

    @Nested
    @DisplayName("Toll-Free Day Tests")
    class TollFreeDayTests {

        @Test
        @DisplayName("Should return zero tax for toll-free days")
        void shouldReturnZeroTaxForTollFreeDays() {
            // Given - Weekend day
            LocalDateTime weekendPassage = LocalDateTime.of(2013, 2, 9, 7, 30); // Saturday
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Collections.singletonList(weekendPassage));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(eq(mockCar), any(LocalDateTime[].class))).thenReturn(0);
            when(taxCalculator.getTollFee(weekendPassage, mockCar)).thenReturn(0);
            when(taxCalculator.isTollFreeDate(weekendPassage)).thenReturn(true);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(0);
            assertThat(response.isTollFreeVehicle()).isFalse();
            assertThat(response.getDailySummaries()).hasSize(1);
            assertThat(response.getDailySummaries().getFirst().isTollFreeDay()).isTrue();
            assertThat(response.getDailySummaries().getFirst().getReason()).isEqualTo("Toll-free day (weekend/holiday/July)");
        }

        @Test
        @DisplayName("Should handle July passages as toll-free")
        void shouldHandleJulyPassagesAsTollFree() {
            // Given - July passage
            LocalDateTime julyPassage = LocalDateTime.of(2013, 7, 15, 7, 30);
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Collections.singletonList(julyPassage));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(eq(mockCar), any(LocalDateTime[].class))).thenReturn(0);
            when(taxCalculator.getTollFee(julyPassage, mockCar)).thenReturn(0);
            when(taxCalculator.isTollFreeDate(julyPassage)).thenReturn(true);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(0);
            assertThat(response.getDailySummaries().getFirst().getReason()).isEqualTo("Toll-free day (weekend/holiday/July)");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Should handle empty passage times")
        void shouldHandleEmptyPassageTimes() {
            // Given
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Collections.emptyList());

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(0);
            assertThat(response.getDailySummaries()).isEmpty();
            assertThat(response.getPassageCalculations()).isEmpty();
        }

        @Test
        @DisplayName("Should handle passages outside toll hours")
        void shouldHandlePassagesOutsideTollHours() {
            // Given - Late night passage
            LocalDateTime nightPassage = LocalDateTime.of(2013, 2, 8, 23, 30);
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Collections.singletonList(nightPassage));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(eq(mockCar), any(LocalDateTime[].class))).thenReturn(0);
            when(taxCalculator.getTollFee(nightPassage, mockCar)).thenReturn(0);
            when(taxCalculator.isTollFreeDate(nightPassage)).thenReturn(false);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(0);
            assertThat(response.getPassageCalculations()).hasSize(1);
            assertThat(response.getPassageCalculations().getFirst().getReason())
                    .isEqualTo("Outside toll hours (18:30-05:59)");
        }

        @Test
        @DisplayName("Should handle maximum daily cap")
        void shouldHandleMaximumDailyCap() {
            // Given - Multiple passages that would exceed daily cap
            List<LocalDateTime> passages = Arrays.asList(
                    LocalDateTime.of(2013, 2, 8, 6, 30),
                    LocalDateTime.of(2013, 2, 8, 7, 30),
                    LocalDateTime.of(2013, 2, 8, 8, 30),
                    LocalDateTime.of(2013, 2, 8, 15, 30),
                    LocalDateTime.of(2013, 2, 8, 17, 30)
            );

            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", passages);

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(eq(mockCar), any(LocalDateTime[].class))).thenReturn(60); // Max cap
            when(taxCalculator.getTollFee(any(LocalDateTime.class), eq(mockCar)))
                    .thenReturn(13, 18, 8, 18, 13); // Individual fees
            when(taxCalculator.isTollFreeDate(any(LocalDateTime.class))).thenReturn(false);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getTotalTax()).isEqualTo(60); // Should be capped at 60
            assertThat(response.getDailySummaries()).hasSize(1);
            assertThat(response.getDailySummaries().getFirst().getDailyTax()).isEqualTo(60);
        }
    }


    @Nested
    @DisplayName("Validation Tests")
    class ValidationTests {

        @Test
        @DisplayName("Should call validation service")
        void shouldCallValidationService() {
            // Given
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Collections.singletonList(LocalDateTime.of(2013, 2, 8, 7, 30)));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(any(), any())).thenReturn(18);
            when(taxCalculator.getTollFee(any(), any())).thenReturn(18);

            // When
            congestionTaxService.calculateTax(request);

            // Then
            verify(validationService).validateServiceRequest(request);
        }

        @Test
        @DisplayName("Should propagate validation exceptions")
        void shouldPropagateValidationExceptions() {
            // Given
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Collections.singletonList(LocalDateTime.of(2013, 2, 8, 7, 30)));

            RuntimeException validationException = new IllegalArgumentException("Invalid request");
            doThrow(validationException).when(validationService).validateServiceRequest(request);

            // When & Then
            assertThatThrownBy(() -> congestionTaxService.calculateTax(request))
                    .isSameAs(validationException);
        }
    }

    @Nested
    @DisplayName("Passage Calculation Details")
    class PassageCalculationDetailsTests {

        @Test
        @DisplayName("Should create correct passage calculations")
        void shouldCreateCorrectPassageCalculations() {
            // Given
            LocalDateTime highTollPassage = LocalDateTime.of(2013, 2, 8, 7, 30); // 18 SEK time
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Collections.singletonList(highTollPassage));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(eq(mockCar), any(LocalDateTime[].class))).thenReturn(18);
            when(taxCalculator.getTollFee(highTollPassage, mockCar)).thenReturn(18);
            when(taxCalculator.isTollFreeDate(highTollPassage)).thenReturn(false);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getPassageCalculations()).hasSize(1);
            PassageCalculation calc = response.getPassageCalculations().getFirst();
            assertThat(calc.getPassageTime()).isEqualTo(highTollPassage);
            assertThat(calc.getIndividualFee()).isEqualTo(18);
            assertThat(calc.getEffectiveFee()).isEqualTo(18);
            assertThat(calc.isTollFreeDay()).isFalse();
            assertThat(calc.isIncludedInTotal()).isTrue();
            assertThat(calc.getReason()).isEqualTo("Regular toll period - 18 SEK");
        }
    }

    @Nested
    @DisplayName("Daily Summary Tests")
    class DailySummaryTests {

        @Test
        @DisplayName("Should create correct daily summaries")
        void shouldCreateCorrectDailySummaries() {
            // Given
            LocalDateTime passage1 = LocalDateTime.of(2013, 2, 8, 6, 30);
            LocalDateTime passage2 = LocalDateTime.of(2013, 2, 8, 15, 30);
            TaxCalculationServiceRequest request = new TaxCalculationServiceRequest("Car", Arrays.asList(passage1, passage2));

            when(vehicleFactory.createVehicle("Car")).thenReturn(mockCar);
            when(taxRulesConfig.isTollFreeVehicle("Car")).thenReturn(false);
            when(taxCalculator.getTax(eq(mockCar), any(LocalDateTime[].class))).thenReturn(31);
            when(taxCalculator.getTollFee(any(LocalDateTime.class), eq(mockCar))).thenReturn(13, 18);
            when(taxCalculator.isTollFreeDate(any(LocalDateTime.class))).thenReturn(false);

            // When
            TaxCalculationServiceResponse response = congestionTaxService.calculateTax(request);

            // Then
            assertThat(response.getDailySummaries()).hasSize(1);
            DailyTaxSummary summary = response.getDailySummaries().getFirst();
            assertThat(summary.getDailyTax()).isEqualTo(31);
            assertThat(summary.getPassageCount()).isEqualTo(2);
            assertThat(summary.isTollFreeDay()).isFalse();
            assertThat(summary.getReason()).isEqualTo("Regular toll day");
            assertThat(summary.getDate()).isEqualTo(LocalDate.of(2013, 2, 8));
        }
    }
}