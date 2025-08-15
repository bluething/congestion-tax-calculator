package io.github.bluething.congestion.calculator.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TaxCalculationServiceRequest {
    private String vehicleType;
    private List<LocalDateTime> passageTimes;

    public TaxCalculationServiceRequest() {}

    public TaxCalculationServiceRequest(String vehicleType, List<LocalDateTime> passageTimes) {
        this.vehicleType = vehicleType;
        this.passageTimes = passageTimes;
    }
}
