package io.github.bluething.congestion.calculator.domain;

public interface TaxService {
    TaxCalculationServiceResponse calculateTax(TaxCalculationServiceRequest request);
}
