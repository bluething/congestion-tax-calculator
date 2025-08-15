package io.github.bluething.congestion.calculator.rest;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
record TaxCalculationRequest(@NotNull(message = "Vehicle type is required")
                             @Pattern(regexp = "Car|Motorcycle|Tractor|Emergency|Diplomat|Foreign|Military",
                                     message = "Vehicle type must be one of: Car, Motorcycle, Tractor, Emergency, Diplomat, Foreign, Military")
                             String vehicleType,

                             @NotEmpty(message = "At least one passage time is required")
                             @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
                             List<LocalDateTime> passageTimes) {}
