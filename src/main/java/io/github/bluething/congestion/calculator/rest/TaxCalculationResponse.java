package io.github.bluething.congestion.calculator.rest;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

record TaxCalculationResponse(String vehicleType,
                                     int totalTax,
                                     boolean tollFreeVehicle,
                                     List<PassageDetail> passageDetails,
                                     @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                                     LocalDateTime calculatedAt) {
    // Factory method to create response with current timestamp
    public static TaxCalculationResponse of(String vehicleType, int totalTax,
                                            boolean tollFreeVehicle, List<PassageDetail> passageDetails) {
        return new TaxCalculationResponse(vehicleType, totalTax, tollFreeVehicle, passageDetails, LocalDateTime.now());
    }

    public record PassageDetail(
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            LocalDateTime passageTime,
            int individualFee,
            boolean tollFreeDay,
            String reason
    ) {}
}
