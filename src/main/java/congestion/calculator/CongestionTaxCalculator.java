package congestion.calculator;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.*;

public class CongestionTaxCalculator {

    private static final Map<String, Integer> tollFreeVehicles = new HashMap<>();
    private static final int SINGLE_CHARGE_INTERVAL_MINUTES = 60;
    private static final int MAX_DAILY_TAX = 60;

    static {
        tollFreeVehicles.put("Motorcycle", 0);
        tollFreeVehicles.put("Tractor", 1);
        tollFreeVehicles.put("Emergency", 2);
        tollFreeVehicles.put("Diplomat", 3);
        tollFreeVehicles.put("Foreign", 4);
        tollFreeVehicles.put("Military", 5);

    }
    
    public int getTax(Vehicle vehicle, LocalDateTime[] dates) {
        if (dates == null || dates.length == 0) {
            return 0;
        }

        Arrays.sort(dates);
        LocalDateTime intervalStart = dates[0];
        int totalFee = 0;
        int tempFee = getTollFee(dates[0], vehicle);

        for (LocalDateTime date : dates) {
            int nextFee = getTollFee(date, vehicle);
            long minutes = Duration.between(intervalStart, date).toMinutes();

            if (minutes <= SINGLE_CHARGE_INTERVAL_MINUTES) {
                if (nextFee > tempFee) {
                    tempFee = nextFee;
                }
            } else {
                totalFee += tempFee;
                intervalStart = date;
                tempFee = nextFee;
            }
        }

        totalFee += tempFee;

        return Math.min(totalFee, MAX_DAILY_TAX);
    }

    private boolean isTollFreeVehicle(Vehicle vehicle) {
        if (vehicle == null) return false;
        String vehicleType = vehicle.getVehicleType();
        return tollFreeVehicles.containsKey(vehicleType);
    }

    public int getTollFee(LocalDateTime date, Vehicle vehicle)
    {
        if (isTollFreeDate(date) || isTollFreeVehicle(vehicle)) return 0;

        int hour = date.getHour();
        int minute = date.getMinute();

        // 06:00–06:29: SEK 8
        if (hour == 6 && minute <= 29) return 8;
        // 06:30–06:59: SEK 13
        else if (hour == 6) return 13;
        // 07:00–07:59: SEK 18
        else if (hour == 7) return 18;
        // 08:00–08:29: SEK 13
        else if (hour == 8 && minute <= 29) return 13;
        // 08:30–14:59: SEK 8
        else if (hour == 8 || (hour >= 9 && hour <= 14)) return 8;
        // 15:00–15:29: SEK 13
        else if (hour == 15 && minute <= 29) return 13;
        // 15:30–16:59: SEK 18
        else if (hour == 15 || hour == 16) return 18;
        // 17:00–17:59: SEK 13
        else if (hour == 17) return 13;
        // 18:00–18:29: SEK 8
        else if (hour == 18 && minute <= 29) return 8;
        // 18:30–05:59: SEK 0 (free)
        else return 0;
    }

    private boolean isTollFreeDate(LocalDateTime date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true;
        }

        int year = date.getYear();
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();

        if (7 == month) {
            return true;
        }

        if (2013 == year) {
            return isSwedishHoliday2013(month, dayOfMonth) || isDayBeforeHoliday2013(month, dayOfMonth);
        }

        return false;
    }
    private boolean isSwedishHoliday2013(int month, int dayOfMonth) {
        // Public holidays in Sweden 2013
        List<String> holidays2013 = Arrays.asList(
                "1-1",   // New Year's Day
                "3-29",  // Good Friday
                "4-1",   // Easter Monday
                "5-1",   // Labour Day
                "5-9",   // Ascension Day
                "6-6",   // National Day
                "6-21",  // Midsummer Eve
                "11-1",  // All Saints' Day
                "12-24", // Christmas Eve
                "12-25", // Christmas Day
                "12-26", // Boxing Day
                "12-31"  // New Year's Eve
        );

        String dateKey = month + "-" + dayOfMonth;
        return holidays2013.contains(dateKey);
    }
    private boolean isDayBeforeHoliday2013(int month, int dayOfMonth) {
        // Days before holidays are also toll-free
        List<String> daysBefore2013 = Arrays.asList(
                "3-28",  // Day before Good Friday
                "4-30",  // Day before Labour Day (when Labour Day falls on weekday)
                "5-8",   // Day before Ascension Day
                "6-5"    // Day before National Day
        );

        String dateKey = month + "-" + dayOfMonth;
        return daysBefore2013.contains(dateKey);
    }
}
