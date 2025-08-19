package io.github.bluething.congestion.calculator.domain;

import java.util.List;
import java.util.Map;

public interface TaxService {
    TaxCalculationServiceResponse calculateTax(TaxCalculationServiceRequest request);
    Map<String, Object> getTollSchedule();
    List<String> getSupportedVehicleTypes();
}
