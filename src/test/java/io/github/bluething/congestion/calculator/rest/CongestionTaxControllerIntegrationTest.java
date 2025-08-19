package io.github.bluething.congestion.calculator.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Congestion Tax Calculator Integration Tests")
class CongestionTaxControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /calculate - Car with multiple passages should return correct tax")
    void calculateTax_withValidCarRequest_shouldReturnCorrectTax() throws Exception {
        String requestJson = """
            {
                "vehicleType": "Car",
                "passageTimes": [
                    "2013-02-08T06:27:00",
                    "2013-02-08T15:29:00",
                    "2013-02-08T16:01:00"
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/congestion-tax/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("Car")))
                .andExpect(jsonPath("$.totalTax", greaterThan(0)))
                .andExpect(jsonPath("$.tollFreeVehicle", is(false)))
                .andExpect(jsonPath("$.passageDetails", hasSize(3)))
                .andExpect(jsonPath("$.calculatedAt", notNullValue()));
    }

    @Test
    @DisplayName("POST /calculate - Motorcycle should return zero tax")
    void calculateTax_withMotorcycle_shouldReturnZeroTax() throws Exception {
        String requestJson = """
            {
                "vehicleType": "Motorcycle",
                "passageTimes": ["2013-02-08T07:30:00"]
            }
            """;

        mockMvc.perform(post("/api/v1/congestion-tax/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("Motorcycle")))
                .andExpect(jsonPath("$.totalTax", is(0)))
                .andExpect(jsonPath("$.tollFreeVehicle", is(true)));
    }

    @Test
    @DisplayName("POST /calculate - Holiday should return zero tax")
    void calculateTax_withHoliday_shouldReturnZeroTax() throws Exception {
        String requestJson = """
            {
                "vehicleType": "Car",
                "passageTimes": ["2013-03-29T07:30:00"]
            }
            """;

        mockMvc.perform(post("/api/v1/congestion-tax/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTax", is(0)))
                .andExpect(jsonPath("$.passageDetails[0].tollFreeDay", is(true)));
    }

    @Test
    @DisplayName("POST /calculate - Invalid vehicle type should return 400")
    void calculateTax_withInvalidVehicleType_shouldReturnBadRequest() throws Exception {
        String requestJson = """
            {
                "vehicleType": "Spaceship",
                "passageTimes": ["2013-02-08T07:30:00"]
            }
            """;

        mockMvc.perform(post("/api/v1/congestion-tax/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", is("VALIDATION_ERROR")));
    }

    @Test
    @DisplayName("GET /calculate - Simple calculation should work")
    void calculateTaxSimple_withValidParameters_shouldReturnCorrectTax() throws Exception {
        mockMvc.perform(get("/api/v1/congestion-tax/calculate")
                        .param("vehicleType", "Car")
                        .param("passageTimes", "2013-02-08T06:27:00,2013-02-08T15:29:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("Car")))
                .andExpect(jsonPath("$.totalTax", greaterThan(0)));
    }

    @Test
    @DisplayName("GET /calculate - Invalid date format should return 400")
    void calculateTaxSimple_withInvalidDateFormat_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/congestion-tax/calculate")
                        .param("vehicleType", "Car")
                        .param("passageTimes", "invalid-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode", anyOf(
                        is("INVALID_DATE_FORMAT"),
                        is("MALFORMED_REQUEST")
                )));
    }

    @Test
    @DisplayName("GET /vehicle-types - Should return all supported types")
    void getSupportedVehicleTypes_shouldReturnAllTypes() throws Exception {
        mockMvc.perform(get("/api/v1/congestion-tax/vehicle-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$", hasItem("Car")))
                .andExpect(jsonPath("$", hasItem("Motorcycle")));
    }

    @Test
    @DisplayName("GET /toll-schedule - Should return complete schedule")
    void getTollSchedule_shouldReturnScheduleInformation() throws Exception {
        mockMvc.perform(get("/api/v1/congestion-tax/toll-schedule"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.maxDailyAmount", is(60)))
                .andExpect(jsonPath("$.currency", is("SEK")))
                .andExpect(jsonPath("$.timeSlots", isA(java.util.List.class)))
                .andExpect(jsonPath("$.tollFreeVehicles", isA(java.util.List.class)));
    }

    @Test
    @DisplayName("POST /calculate - Colleague's post-it data test")
    void calculateTax_withColleagueData_shouldReturnExpectedResults() throws Exception {
        String requestJson = """
            {
                "vehicleType": "Car",
                "passageTimes": [
                    "2013-02-07T06:23:27",
                    "2013-02-07T15:27:00",
                    "2013-02-08T06:27:00",
                    "2013-02-08T15:29:00",
                    "2013-02-08T16:01:00"
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/congestion-tax/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vehicleType", is("Car")))
                .andExpect(jsonPath("$.totalTax", greaterThan(0)))
                .andExpect(jsonPath("$.passageDetails", hasSize(5)))
                .andExpect(jsonPath("$.dailyTaxSummaries", hasSize(2))); // Two different days
    }

    @Test
    @DisplayName("POST /calculate - Daily maximum test")
    void calculateTax_withManyPassages_shouldCapAtDailyMaximum() throws Exception {
        String requestJson = """
            {
                "vehicleType": "Car",
                "passageTimes": [
                    "2013-02-08T07:00:00",
                    "2013-02-08T09:00:00",
                    "2013-02-08T11:00:00",
                    "2013-02-08T13:00:00",
                    "2013-02-08T15:30:00",
                    "2013-02-08T17:00:00"
                ]
            }
            """;

        mockMvc.perform(post("/api/v1/congestion-tax/calculate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTax", lessThanOrEqualTo(60))); // Should not exceed daily max
    }
}