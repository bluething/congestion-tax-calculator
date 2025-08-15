package io.github.bluething.congestion.calculator.domain;

import io.github.bluething.congestion.calculator.exception.InvalidDateFormatException;
import io.github.bluething.congestion.calculator.exception.InvalidVehicleTypeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
class ValidationService {
    private static final int MAX_PASSAGES_PER_REQUEST = 100;
    private static final int MAX_DAYS_SPAN = 7;

    private final TaxRulesConfig taxRulesConfig;

    /**
     * Validate service layer request DTO
     */
    public void validateServiceRequest(TaxCalculationServiceRequest request) {
        log.debug("Validating service tax calculation request");

        if (request == null) {
            throw new InvalidVehicleTypeException("Request cannot be null");
        }

        validateVehicleType(request.getVehicleType());
        validatePassageTimes(request.getPassageTimes());
        validateBusinessRules(request);

        log.debug("Service tax calculation request validation passed");
    }

    private void validateVehicleType(String vehicleType) {
        if (!StringUtils.hasText(vehicleType)) {
            throw new InvalidVehicleTypeException("Vehicle type cannot be null or empty");
        }

        if (!taxRulesConfig.isValidVehicleType(vehicleType)) {
            throw new InvalidVehicleTypeException(
                    String.format("Invalid vehicle type '%s'. Supported types: %s",
                            vehicleType, taxRulesConfig.getAllVehicleTypes())
            );
        }
    }

    private void validatePassageTimes(List<LocalDateTime> passageTimes) {
        if (CollectionUtils.isEmpty(passageTimes)) {
            throw new InvalidDateFormatException("At least one passage time is required");
        }

        if (passageTimes.size() > MAX_PASSAGES_PER_REQUEST) {
            throw new InvalidDateFormatException(
                    String.format("Too many passage times. Maximum allowed: %d", MAX_PASSAGES_PER_REQUEST)
            );
        }

        // Check for null values
        if (passageTimes.stream().anyMatch(java.util.Objects::isNull)) {
            throw new InvalidDateFormatException("Passage times cannot contain null values");
        }

        // Validate date range
        validateDateRange(passageTimes);

        // Check for reasonable future dates
        validateFutureDates(passageTimes);
    }

    private void validateDateRange(List<LocalDateTime> passageTimes) {
        if (passageTimes.size() < 2) return;

        LocalDateTime earliest = passageTimes.stream().min(LocalDateTime::compareTo).orElseThrow();
        LocalDateTime latest = passageTimes.stream().max(LocalDateTime::compareTo).orElseThrow();

        long daysBetween = ChronoUnit.DAYS.between(earliest.toLocalDate(), latest.toLocalDate());

        if (daysBetween > MAX_DAYS_SPAN) {
            throw new InvalidDateFormatException(
                    String.format("Passage times span too many days (%d). Maximum allowed: %d days",
                            daysBetween, MAX_DAYS_SPAN)
            );
        }
    }

    private void validateFutureDates(List<LocalDateTime> passageTimes) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime maxFutureDate = now.plusYears(1);

        boolean hasFarFutureDates = passageTimes.stream()
                .anyMatch(time -> time.isAfter(maxFutureDate));

        if (hasFarFutureDates) {
            log.warn("Request contains dates more than 1 year in the future");
            // Note: We're logging a warning but not throwing an exception
            // This allows for testing with future dates while flagging unusual usage
        }
    }

    private void validateBusinessRules(TaxCalculationServiceRequest request) {
        // Check for year compatibility (currently limited to 2013)
        List<LocalDateTime> passageTimes = request.getPassageTimes();

        boolean hasNon2013Dates = passageTimes.stream()
                .anyMatch(time -> time.getYear() != 2013);

        if (hasNon2013Dates) {
            log.warn("Request contains dates outside of 2013. Holiday rules may not apply correctly.");
            // Note: Warning only, as the assignment scope is limited to 2013
        }
    }

    public void validateSimpleRequest(String vehicleType, String passageTimesStr) {
        if (!StringUtils.hasText(vehicleType)) {
            throw new InvalidVehicleTypeException("Vehicle type cannot be empty");
        }

        if (!StringUtils.hasText(passageTimesStr)) {
            throw new InvalidDateFormatException("Passage times cannot be empty");
        }

        if (passageTimesStr.split(",").length > MAX_PASSAGES_PER_REQUEST) {
            throw new InvalidDateFormatException(
                    String.format("Too many passage times. Maximum allowed: %d", MAX_PASSAGES_PER_REQUEST)
            );
        }
    }
}
