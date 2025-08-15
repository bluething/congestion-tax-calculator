package io.github.bluething.congestion.calculator.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class PassageCalculation {
    private LocalDateTime passageTime;
    private int individualFee;
    private int effectiveFee; // Fee after 60-minute rule applied
    private boolean tollFreeDay;
    private boolean includedInTotal;
    private String reason;

    public PassageCalculation() {}

    public PassageCalculation(LocalDateTime passageTime, int individualFee, int effectiveFee,
                              boolean tollFreeDay, boolean includedInTotal, String reason) {
        this.passageTime = passageTime;
        this.individualFee = individualFee;
        this.effectiveFee = effectiveFee;
        this.tollFreeDay = tollFreeDay;
        this.includedInTotal = includedInTotal;
        this.reason = reason;
    }
}
