package io.github.bluething.congestion.calculator.rest;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/congestion-tax")
class CongestionTaxController {

    @PostMapping("/calculate")
    public ResponseEntity<TaxCalculationResponse> calculateTax(
            @Valid @RequestBody TaxCalculationRequest request) {
        // TODO
        return null;
    }
}
