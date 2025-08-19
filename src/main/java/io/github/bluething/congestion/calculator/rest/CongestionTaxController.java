package io.github.bluething.congestion.calculator.rest;

import io.github.bluething.congestion.calculator.domain.TaxCalculationServiceRequest;
import io.github.bluething.congestion.calculator.domain.TaxCalculationServiceResponse;
import io.github.bluething.congestion.calculator.domain.TaxService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/congestion-tax")
@RequiredArgsConstructor
@Slf4j
class CongestionTaxController {

    private final TaxService taxService;
    private final DTOMapper dtoMapper;

    @PostMapping("/calculate")
    @Operation(
            summary = "Calculate congestion tax for a vehicle",
            description = "Calculate the total congestion tax for a vehicle based on passage times. " +
                    "Automatically groups passages by day and applies daily maximum (60 SEK per day). " +
                    "Returns detailed breakdown including individual fees, daily summaries, and toll-free periods.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    content = @Content(
                            examples = @ExampleObject(
                                    name = "Car with multiple passages across different days",
                                    value = """
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
                                            """
                            )
                    )
            )
    )
    @ApiResponse(responseCode = "200", description = "Tax calculation successful")
    @ApiResponse(responseCode = "400", description = "Invalid request data")
    public ResponseEntity<TaxCalculationResponse> calculateTax(
            @Valid @RequestBody TaxCalculationRequest webRequest) {

        log.info("Received tax calculation request for vehicle: {}", webRequest.vehicleType());

        TaxCalculationServiceRequest serviceRequest = dtoMapper.toServiceRequest(webRequest);

        TaxCalculationServiceResponse serviceResponse = taxService.calculateTax(serviceRequest);

        TaxCalculationResponse webResponse = dtoMapper.toWebResponse(serviceResponse);

        log.info("Tax calculation completed for vehicle: {}, Total: {} SEK",
                webRequest.vehicleType(), webResponse.totalTax());

        return ResponseEntity.ok(webResponse);
    }

    @GetMapping("/calculate")
    @Operation(
            summary = "Calculate congestion tax via GET request",
            description = "Simple GET endpoint for quick tax calculations. " +
                    "Automatically groups passages by day for proper daily maximum calculation."
    )
    public ResponseEntity<TaxCalculationResponse> calculateTaxSimple(
            @Parameter(description = "Vehicle type", example = "Car")
            @RequestParam String vehicleType,

            @Parameter(description = "Comma-separated list of passage times in format yyyy-MM-dd HH:mm:ss",
                    example = "2013-02-07 06:23:27,2013-02-08 15:29:00")
            @RequestParam String passageTimes) {

        log.info("Received simple tax calculation request for vehicle: {}", vehicleType);

        try {
            List<LocalDateTime> parsedTimes = Arrays.stream(passageTimes.split(","))
                    .map(String::trim)
                    .map(LocalDateTime::parse)
                    .collect(Collectors.toList());

            TaxCalculationRequest webRequest = new TaxCalculationRequest(vehicleType, parsedTimes);

            TaxCalculationServiceRequest serviceRequest = dtoMapper.toServiceRequest(webRequest);

            TaxCalculationServiceResponse serviceResponse = taxService.calculateTax(serviceRequest);

            TaxCalculationResponse webResponse = dtoMapper.toWebResponse(serviceResponse);

            log.info("Simple tax calculation completed for vehicle: {}, Total: {} SEK",
                    vehicleType, webResponse.totalTax());

            return ResponseEntity.ok(webResponse);

        } catch (Exception e) {
            log.error("Error in simple tax calculation: {}", e.getMessage());
            throw e; // Let global exception handler deal with it
        }
    }
}
