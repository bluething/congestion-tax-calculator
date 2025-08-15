package io.github.bluething.congestion.calculator.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
class CongestionTaxService implements TaxService {
    private final TaxCalculator taxCalculator;
    private final TaxRulesConfig taxRulesConfig;
    private final ValidationService validationService;
    private final VehicleFactory vehicleFactory;

    @Override
    public TaxCalculationServiceResponse calculateTax(TaxCalculationServiceRequest request) {
        log.debug("Calculating tax for vehicle type: {} with {} passage times",
                request.getVehicleType(), request.getPassageTimes().size());

        validationService.validateServiceRequest(request);

        Vehicle vehicle = vehicleFactory.createVehicle(request.getVehicleType());

        boolean isTollFreeVehicle = taxRulesConfig.isTollFreeVehicle(request.getVehicleType());

        return calculateTaxGroupedByDay(request, vehicle, isTollFreeVehicle);
    }

    /**
     * Calculate tax with proper daily grouping (recommended approach)
     */
    private TaxCalculationServiceResponse calculateTaxGroupedByDay(
            TaxCalculationServiceRequest request, Vehicle vehicle, boolean isTollFreeVehicle) {

        // Group passages by day
        Map<LocalDate, List<LocalDateTime>> passagesByDay = request.getPassageTimes()
                .stream()
                .collect(Collectors.groupingBy(LocalDateTime::toLocalDate));

        List<DailyTaxSummary> dailySummaries = new ArrayList<>();
        List<PassageCalculation> allPassageCalculations = new ArrayList<>();
        int totalTax = 0;

        for (Map.Entry<LocalDate, List<LocalDateTime>> dayEntry : passagesByDay.entrySet()) {
            LocalDate date = dayEntry.getKey();
            List<LocalDateTime> dayPassages = dayEntry.getValue();

            log.debug("Calculating tax for {} with {} passages", date, dayPassages.size());

            // Calculate tax for this day
            LocalDateTime[] dayPassagesArray = dayPassages.toArray(new LocalDateTime[0]);
            int dailyTax = taxCalculator.getTax(vehicle, dayPassagesArray);

            // Create passage calculations for this day
            List<PassageCalculation> dayCalculations = createPassageCalculations(
                    vehicle, dayPassages, isTollFreeVehicle);
            allPassageCalculations.addAll(dayCalculations);

            // Create daily summary
            boolean isTollFreeDay = dailyTax == 0 && !isTollFreeVehicle && !dayPassages.isEmpty();
            String reason = determineDayReason(isTollFreeVehicle, isTollFreeDay);

            DailyTaxSummary dailySummary = new DailyTaxSummary(
                    dayPassages.getFirst(), // Use first passage time for date reference
                    dailyTax,
                    dayPassages.size(),
                    isTollFreeDay,
                    reason
            );
            dailySummaries.add(dailySummary);

            totalTax += dailyTax;

            log.debug("Daily tax for {}: {} SEK", date, dailyTax);
        }

        log.debug("Total tax across all days: {} SEK", totalTax);

        return new TaxCalculationServiceResponse(
                request.getVehicleType(),
                totalTax,
                isTollFreeVehicle,
                dailySummaries,
                allPassageCalculations
        );
    }

    private List<PassageCalculation> createPassageCalculations(
            Vehicle vehicle, List<LocalDateTime> passageTimes, boolean isTollFreeVehicle) {

        return passageTimes.stream()
                .map(time -> {
                    int individualFee = taxCalculator.getTollFee(time, vehicle);
                    boolean isTollFreeDay = taxCalculator.isTollFreeDate(time);
                    String reason = determinePassageReason(vehicle, individualFee, isTollFreeDay, isTollFreeVehicle);

                    return new PassageCalculation(
                            time,
                            individualFee,
                            individualFee, // For now, same as individual (60-minute rule is complex to track)
                            isTollFreeDay,
                            individualFee > 0,
                            reason
                    );
                })
                .collect(Collectors.toList());
    }

    private String determineDayReason(boolean isTollFreeVehicle, boolean isTollFreeDay) {
        if (isTollFreeVehicle) {
            return "Toll-free vehicle type";
        }
        if (isTollFreeDay) {
            return "Toll-free day (weekend/holiday/July)";
        }
        return "Regular toll day";
    }

    private String determinePassageReason(Vehicle vehicle, int fee,
                                          boolean isTollFreeDay, boolean isTollFreeVehicle) {
        if (isTollFreeVehicle) {
            return "Toll-free vehicle type: " + vehicle.getVehicleType();
        }
        if (isTollFreeDay) {
            return "Toll-free day (weekend/holiday/July)";
        }
        if (fee == 0) {
            return "Outside toll hours (18:30-05:59)";
        }
        return "Regular toll period - " + fee + " SEK";
    }
}
