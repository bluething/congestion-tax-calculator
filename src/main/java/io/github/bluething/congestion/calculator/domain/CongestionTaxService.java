package io.github.bluething.congestion.calculator.domain;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
class CongestionTaxService implements TaxService {
    @Override
    public TaxCalculationServiceResponse calculateTax(TaxCalculationServiceRequest request) {
        return null;
    }
}
