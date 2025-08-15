package io.github.bluething.congestion.calculator.domain;

import java.time.LocalDateTime;

public interface TaxCalculator {
    int getTax(Vehicle vehicle, LocalDateTime[] dates);
}
