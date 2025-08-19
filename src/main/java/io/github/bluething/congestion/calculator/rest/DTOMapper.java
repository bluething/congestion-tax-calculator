package io.github.bluething.congestion.calculator.rest;

import io.github.bluething.congestion.calculator.domain.DailyTaxSummary;
import io.github.bluething.congestion.calculator.domain.PassageCalculation;
import io.github.bluething.congestion.calculator.domain.TaxCalculationServiceRequest;
import io.github.bluething.congestion.calculator.domain.TaxCalculationServiceResponse;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
class DTOMapper {
    /**
     * Convert web request DTO to service request DTO
     */
    public TaxCalculationServiceRequest toServiceRequest(TaxCalculationRequest webRequest) {
        if (webRequest == null) {
            return null;
        }

        return new TaxCalculationServiceRequest(
                webRequest.vehicleType(),
                webRequest.passageTimes()
        );
    }

    /**
     * Convert service response DTO to web response DTO
     */
    public TaxCalculationResponse toWebResponse(TaxCalculationServiceResponse serviceResponse) {
        if (serviceResponse == null) {
            return null;
        }

        // Convert passage calculations to web format
        List<TaxCalculationResponse.PassageDetail> passageDetails = serviceResponse.getPassageCalculations()
                .stream()
                .map(this::toWebPassageDetail)
                .collect(Collectors.toList());

        return TaxCalculationResponse.of(
                serviceResponse.getVehicleType(),
                serviceResponse.getTotalTax(),
                serviceResponse.isTollFreeVehicle(),
                passageDetails,
                convertDailySummaries(serviceResponse.getDailySummaries())
        );
    }

    /**
     * Convert service passage calculation to web passage detail
     */
    private TaxCalculationResponse.PassageDetail toWebPassageDetail(
            PassageCalculation serviceCalculation) {

        return new TaxCalculationResponse.PassageDetail(
                serviceCalculation.getPassageTime(),
                serviceCalculation.getIndividualFee(),
                serviceCalculation.isTollFreeDay(),
                serviceCalculation.getReason()
        );
    }

    /**
     * Convert daily summaries for web response (optional enhancement)
     */
    private List<TaxCalculationResponse.DailyTaxSummaryInfo> convertDailySummaries(
            List<DailyTaxSummary> dailySummaries) {

        return dailySummaries.stream()
                .map(summary -> new TaxCalculationResponse.DailyTaxSummaryInfo(
                        summary.getDate(),
                        summary.getDailyTax(),
                        summary.getPassageCount(),
                        summary.isTollFreeDay(),
                        summary.getReason()
                ))
                .collect(Collectors.toList());
    }
}
