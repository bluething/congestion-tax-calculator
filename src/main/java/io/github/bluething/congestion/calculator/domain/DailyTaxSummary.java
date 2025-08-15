package io.github.bluething.congestion.calculator.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class DailyTaxSummary {
    private LocalDate date;
    private int dailyTax;
    private int passageCount;
    private boolean tollFreeDay;
    private String reason;

    public DailyTaxSummary() {}

    public DailyTaxSummary(LocalDate date, int dailyTax, int passageCount, boolean tollFreeDay, String reason) {
        this.date = date;
        this.dailyTax = dailyTax;
        this.passageCount = passageCount;
        this.tollFreeDay = tollFreeDay;
        this.reason = reason;
    }

    // Convenience constructor that extracts date from LocalDateTime
    public DailyTaxSummary(LocalDateTime dateTime, int dailyTax, int passageCount, boolean tollFreeDay, String reason) {
        this(dateTime.toLocalDate(), dailyTax, passageCount, tollFreeDay, reason);
    }
}
