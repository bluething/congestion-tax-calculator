package io.github.bluething.congestion.calculator.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class TaxCalculationResponse {
    private String vehicleType;
    private int totalTax;
    private boolean tollFreeVehicle;
    private List<PassageDetail> passageDetails;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime calculatedAt;

    public TaxCalculationResponse() {
        this.calculatedAt = LocalDateTime.now();
    }

    public TaxCalculationResponse(String vehicleType, int totalTax, boolean tollFreeVehicle,
                                  List<PassageDetail> passageDetails) {
        this();
        this.vehicleType = vehicleType;
        this.totalTax = totalTax;
        this.tollFreeVehicle = tollFreeVehicle;
        this.passageDetails = passageDetails;
    }

    @Getter
    @Setter
    public static class PassageDetail {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime passageTime;
        private int individualFee;
        private boolean tollFreeDay;
        private String reason;

        public PassageDetail() {
        }

        public PassageDetail(LocalDateTime passageTime, int individualFee, boolean tollFreeDay, String reason) {
            this.passageTime = passageTime;
            this.individualFee = individualFee;
            this.tollFreeDay = tollFreeDay;
            this.reason = reason;
        }
    }
}
