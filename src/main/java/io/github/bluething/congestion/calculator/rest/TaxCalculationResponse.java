package io.github.bluething.congestion.calculator.rest;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

record TaxCalculationResponse(String vehicleType,
                              int totalTax,
                              boolean tollFreeVehicle,
                              List<PassageDetail> passageDetails,
                              List<DailyTaxSummaryInfo> dailyTaxSummaries,
                              LocalDateTime calculatedAt) {
    // Factory method to create response with current timestamp
    public static TaxCalculationResponse of(String vehicleType, int totalTax,
                                            boolean tollFreeVehicle, List<PassageDetail> passageDetails, List<DailyTaxSummaryInfo> dailyTaxSummaries) {
        return new TaxCalculationResponse(vehicleType, totalTax, tollFreeVehicle, passageDetails, dailyTaxSummaries, LocalDateTime.now());
    }

    record PassageDetail(
            LocalDateTime passageTime,
            int individualFee,
            boolean tollFreeDay,
            String reason
    ) {}
    record DailyTaxSummaryInfo(LocalDate date,
            int dailyTax,
            int passageCount,
            boolean tollFreeDay,
            String reason) {}
}
