package com.homeops.backend.maintenanceevent;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MaintenanceEventResponse(
        UUID id,
        UUID householdId,
        UUID vehicleId,
        LocalDate serviceDate,
        String description,
        Integer mileage,
        BigDecimal cost,
        String notes,
        Instant createdAt,
        Instant updatedAt
) {
    static MaintenanceEventResponse from(MaintenanceEvent maintenanceEvent) {
        return new MaintenanceEventResponse(
                maintenanceEvent.getId(),
                maintenanceEvent.getVehicle().getHousehold().getId(),
                maintenanceEvent.getVehicle().getId(),
                maintenanceEvent.getServiceDate(),
                maintenanceEvent.getDescription(),
                maintenanceEvent.getMileage(),
                maintenanceEvent.getCost(),
                maintenanceEvent.getNotes(),
                maintenanceEvent.getCreatedAt(),
                maintenanceEvent.getUpdatedAt()
        );
    }
}
