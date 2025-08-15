package io.github.bluething.congestion.calculator.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConfigurationProperties(prefix = "congestion.tax")
@Getter
@Setter
class TaxRulesConfig {
    private int maxDailyTax = 60;
    private int singleChargeIntervalMinutes = 60;
    private List<Integer> tollFreeMonths = List.of(7); // July

    private Set<String> tollFreeVehicles = Set.of(
            "Motorcycle", "Tractor", "Emergency", "Diplomat", "Foreign", "Military"
    );

    private List<String> allVehicleTypes = Arrays.asList(
            "Car", "Motorcycle", "Tractor", "Emergency", "Diplomat", "Foreign", "Military"
    );

    // Time slots with their corresponding fees
    private Map<String, Integer> timeSlots = new LinkedHashMap<>() {{
        put("06:00-06:29", 8);
        put("06:30-06:59", 13);
        put("07:00-07:59", 18);
        put("08:00-08:29", 13);
        put("08:30-14:59", 8);
        put("15:00-15:29", 13);
        put("15:30-16:59", 18);
        put("17:00-17:59", 13);
        put("18:00-18:29", 8);
        put("18:30-05:59", 0);
    }};

    // 2013 Swedish holidays (can be moved to external configuration later)
    private Map<String, List<String>> holidays = Map.of(
            "2013", Arrays.asList(
                    "01-01", // New Year's Day
                    "03-29", // Good Friday
                    "04-01", // Easter Monday
                    "05-01", // Labour Day
                    "05-09", // Ascension Day
                    "06-06", // National Day
                    "06-21", // Midsummer Eve
                    "11-01", // All Saints' Day
                    "12-24", // Christmas Eve
                    "12-25", // Christmas Day
                    "12-26", // Boxing Day
                    "12-31"  // New Year's Eve
            )
    );

    private Map<String, List<String>> daysBeforeHolidays = Map.of(
            "2013", Arrays.asList(
                    "03-28", // Day before Good Friday
                    "04-30", // Day before Labour Day
                    "05-08", // Day before Ascension Day
                    "06-05"  // Day before National Day
            )
    );

    public boolean isTollFreeMonth(int month) {
        return tollFreeMonths.contains(month);
    }

    public boolean isTollFreeVehicle(String vehicleType) {
        return tollFreeVehicles.contains(vehicleType);
    }

    public boolean isValidVehicleType(String vehicleType) {
        return allVehicleTypes.contains(vehicleType);
    }

    public List<String> getHolidaysForYear(int year) {
        return holidays.getOrDefault(String.valueOf(year), Collections.emptyList());
    }

    public List<String> getDaysBeforeHolidaysForYear(int year) {
        return daysBeforeHolidays.getOrDefault(String.valueOf(year), Collections.emptyList());
    }
}
