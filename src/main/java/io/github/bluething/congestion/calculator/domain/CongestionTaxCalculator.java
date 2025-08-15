package io.github.bluething.congestion.calculator.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
class CongestionTaxCalculator implements TaxCalculator {

    private TaxRulesConfig taxRulesConfig;

    @Override
    public int getTax(Vehicle vehicle, LocalDateTime[] dates) {
        if (dates == null || dates.length == 0) {
            log.debug("No dates provided for tax calculation");
            return 0;
        }

        if (isTollFreeVehicle(vehicle)) {
            log.debug("Vehicle type {} is toll-free", vehicle == null ? "unknown" : vehicle.getVehicleType());
            return 0;
        }

        log.debug("Calculating tax for {} passages", dates.length);

        // Sort dates to ensure chronological order
        LocalDateTime[] sortedDates = Arrays.copyOf(dates, dates.length);
        Arrays.sort(sortedDates);

        LocalDateTime intervalStart = sortedDates[0];
        int totalFee = 0;
        int tempFee = getTollFee(intervalStart, vehicle);

        for (LocalDateTime date : sortedDates) {
            int nextFee = getTollFee(date, vehicle);
            long minutes = Duration.between(intervalStart, date).toMinutes();

            if (minutes <= taxRulesConfig.getSingleChargeIntervalMinutes()) {
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

        // Apply daily maximum
        int finalTax = Math.min(totalFee, taxRulesConfig.getMaxDailyTax());

        log.debug("Tax calculation completed. Total before cap: {} SEK, Final: {} SEK",
                totalFee, finalTax);

        return finalTax;
    }

    private boolean isTollFreeVehicle(Vehicle vehicle) {
        if (vehicle == null) return true;
        String vehicleType = vehicle.getVehicleType();
        boolean isTollFree = taxRulesConfig.isTollFreeVehicle(vehicleType);

        log.debug("Vehicle type {} is toll-free: {}", vehicleType, isTollFree);
        return isTollFree;
    }

    @Override
    public int getTollFee(LocalDateTime date, Vehicle vehicle)
    {
        if (date == null) {
            log.warn("Date is null for toll fee calculation");
            return 0;
        }

        if (isTollFreeDate(date) || isTollFreeVehicle(vehicle)) {
            log.debug("Toll-free date or vehicle for {}", date);
            return 0;
        }

        int hour = date.getHour();
        int minute = date.getMinute();

        int fee = calculateHourlyFee(hour, minute);

        log.debug("Toll fee for {} at {}:{} = {} SEK",
                vehicle != null ? vehicle.getVehicleType() : "unknown", hour, minute, fee);
        return fee;
    }
    private int calculateHourlyFee(int hour, int minute) {
        // 06:00–06:29: SEK 8
        if (hour == 6 && minute <= 29) return 8;
            // 06:30–06:59: SEK 13
        else if (hour == 6 && minute >= 30) return 13;
            // 07:00–07:59: SEK 18
        else if (hour == 7) return 18;
            // 08:00–08:29: SEK 13
        else if (hour == 8 && minute <= 29) return 13;
            // 08:30–14:59: SEK 8
        else if ((hour == 8 && minute >= 30) || (hour >= 9 && hour <= 14)) return 8;
            // 15:00–15:29: SEK 13
        else if (hour == 15 && minute <= 29) return 13;
            // 15:30–16:59: SEK 18
        else if ((hour == 15 && minute >= 30) || hour == 16) return 18;
            // 17:00–17:59: SEK 13
        else if (hour == 17) return 13;
            // 18:00–18:29: SEK 8
        else if (hour == 18 && minute <= 29) return 8;
            // 18:30–05:59: SEK 0 (free)
        else return 0;
    }

    @Override
    public boolean isTollFreeDate(LocalDateTime date) {
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            log.debug("Weekend date: {}", date.toLocalDate());
            return true;
        }

        int year = date.getYear();
        int month = date.getMonthValue();
        int dayOfMonth = date.getDayOfMonth();

        // Check toll-free months
        if (taxRulesConfig.isTollFreeMonth(month)) {
            log.debug("Toll-free month: {}", month);
            return true;
        }

        // Check holidays and days before holidays
        boolean isHoliday = isSwedishHoliday(year, month, dayOfMonth) ||
                isDayBeforeHoliday(year, month, dayOfMonth);

        if (isHoliday) {
            log.debug("Holiday or day before holiday: {}-{}-{}", year, month, dayOfMonth);
        }

        return isHoliday;
    }
    private boolean isSwedishHoliday(int year, int month, int dayOfMonth) {
        String dateKey = String.format("%02d-%02d", month, dayOfMonth);
        List<String> holidays = taxRulesConfig.getHolidaysForYear(year);
        return holidays.contains(dateKey);
    }
    private boolean isDayBeforeHoliday(int year, int month, int dayOfMonth) {
        String dateKey = String.format("%02d-%02d", month, dayOfMonth);
        List<String> daysBefore = taxRulesConfig.getDaysBeforeHolidaysForYear(year);
        return daysBefore.contains(dateKey);
    }
}
