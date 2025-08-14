package congestion.calculator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CongestionTaxCalculatorTest {
    public static void main(String[] args) {
        CongestionTaxCalculator calculator = new CongestionTaxCalculator();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Test data from colleague's post-it
        String[] testDates = {
                "2013-01-14 21:00:00", // Monday evening - should be 0 (outside toll hours)
                "2013-01-15 21:00:00", // Tuesday evening - should be 0 (outside toll hours)
                "2013-02-07 06:23:27", // Thursday morning - should be 8 (06:00-06:29)
                "2013-02-07 15:27:00", // Thursday afternoon - should be 13 (15:00-15:29)
                "2013-02-08 06:27:00", // Friday morning - should be 8 (06:00-06:29)
                "2013-02-08 06:20:27", // Friday morning - within 60min, max should be 8
                "2013-02-08 14:35:00", // Friday afternoon - should be 8 (08:30-14:59)
                "2013-02-08 15:29:00", // Friday afternoon - should be 13 (15:00-15:29)
                "2013-02-08 15:47:00", // Friday afternoon - within 60min, max should be 18
                "2013-02-08 16:01:00", // Friday afternoon - should be 18 (15:30-16:59)
                "2013-02-08 16:48:00", // Friday afternoon - within 60min, max should be 18
                "2013-02-08 17:49:00", // Friday evening - should be 13 (17:00-17:59)
                "2013-02-08 18:29:00", // Friday evening - should be 8 (18:00-18:29)
                "2013-02-08 18:35:00", // Friday evening - should be 0 (18:30-05:59)
                "2013-03-26 14:25:00", // Tuesday - should be 8 (08:30-14:59)
                "2013-03-28 14:07:27"  // Thursday (day before Good Friday) - should be 0 (toll-free)
        };

        System.out.println("=== Individual Toll Fee Tests ===");
        Car car = new Car();
        Motorbike motorbike = new Motorbike();

        for (String dateStr : testDates) {
            LocalDateTime date = LocalDateTime.parse(dateStr, formatter);
            int carFee = calculator.getTollFee(date, car);
            int motorbikeFee = calculator.getTollFee(date, motorbike);

            System.out.printf("%s - Car: %d SEK, Motorbike: %d SEK%n",
                    dateStr, carFee, motorbikeFee);
        }

        System.out.println("\n=== Daily Tax Calculation Tests ===");

        // Test single day with multiple passages (2013-02-08)
        LocalDateTime[] feb8Dates = {
                LocalDateTime.parse("2013-02-08 06:27:00", formatter),
                LocalDateTime.parse("2013-02-08 06:20:27", formatter),
                LocalDateTime.parse("2013-02-08 14:35:00", formatter),
                LocalDateTime.parse("2013-02-08 15:29:00", formatter),
                LocalDateTime.parse("2013-02-08 15:47:00", formatter),
                LocalDateTime.parse("2013-02-08 16:01:00", formatter),
                LocalDateTime.parse("2013-02-08 16:48:00", formatter),
                LocalDateTime.parse("2013-02-08 17:49:00", formatter),
                LocalDateTime.parse("2013-02-08 18:29:00", formatter),
                LocalDateTime.parse("2013-02-08 18:35:00", formatter)
        };

        int dailyTax = calculator.getTax(car, feb8Dates);
        System.out.printf("Total tax for 2013-02-08: %d SEK%n", dailyTax);

        // Test toll-free dates
        LocalDateTime[] tollFreeDates = {
                LocalDateTime.parse("2013-01-14 21:00:00", formatter), // Normal day, but outside hours
                LocalDateTime.parse("2013-03-28 14:07:27", formatter), // Day before holiday
                LocalDateTime.parse("2013-07-15 08:00:00", formatter)  // July (toll-free month)
        };

        int tollFreeTax = calculator.getTax(car, tollFreeDates);
        System.out.printf("Tax for toll-free dates: %d SEK%n", tollFreeTax);

        // Test motorbike (should always be 0)
        int motorbikeTax = calculator.getTax(motorbike, feb8Dates);
        System.out.printf("Motorbike tax (should be 0): %d SEK%n", motorbikeTax);
    }
}
