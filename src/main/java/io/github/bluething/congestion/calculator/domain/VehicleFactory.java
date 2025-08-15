package io.github.bluething.congestion.calculator.domain;

import io.github.bluething.congestion.calculator.exception.InvalidVehicleTypeException;
import org.springframework.stereotype.Component;

@Component
class VehicleFactory {
    public Vehicle createVehicle(String vehicleType) {
        if (vehicleType == null || vehicleType.trim().isEmpty()) {
            throw new InvalidVehicleTypeException("Vehicle type cannot be null or empty");
        }

        return switch (vehicleType.trim()) {
            case "Car" -> new Car();
            case "Motorcycle" -> new Motorbike();
            case "Tractor" -> new TollFreeVehicle("Tractor");
            case "Emergency" -> new TollFreeVehicle("Emergency");
            case "Diplomat" -> new TollFreeVehicle("Diplomat");
            case "Foreign" -> new TollFreeVehicle("Foreign");
            case "Military" -> new TollFreeVehicle("Military");
            default -> throw new InvalidVehicleTypeException(
                    "Unsupported vehicle type: " + vehicleType +
                            ". Supported types: Car, Motorcycle, Tractor, Emergency, Diplomat, Foreign, Military"
            );
        };
    }

    // Inner class for toll-free vehicles
    private record TollFreeVehicle(String type) implements Vehicle {
        @Override
            public String getVehicleType() {
                return type;
            }
    }
}
