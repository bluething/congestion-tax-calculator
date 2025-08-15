package io.github.bluething.congestion.calculator.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TaxCalculationServiceResponse {
    private String vehicleType;
    private int totalTax;
    private boolean tollFreeVehicle;
    private List<DailyTaxSummary> dailySummaries;
    private List<PassageCalculation> passageCalculations;

    public TaxCalculationServiceResponse() {}

    public TaxCalculationServiceResponse(String vehicleType, int totalTax, boolean tollFreeVehicle,
                                         List<DailyTaxSummary> dailySummaries,
                                         List<PassageCalculation> passageCalculations) {
        this.vehicleType = vehicleType;
        this.totalTax = totalTax;
        this.tollFreeVehicle = tollFreeVehicle;
        this.dailySummaries = dailySummaries;
        this.passageCalculations = passageCalculations;
    }
}
