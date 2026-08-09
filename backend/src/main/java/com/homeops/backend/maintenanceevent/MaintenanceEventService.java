package com.homeops.backend.maintenanceevent;

import com.homeops.backend.household.Household;
import com.homeops.backend.household.HouseholdNotFoundException;
import com.homeops.backend.household.HouseholdRepository;
import com.homeops.backend.vehicle.Vehicle;
import com.homeops.backend.vehicle.VehicleNotFoundException;
import com.homeops.backend.vehicle.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class MaintenanceEventService {

    private final MaintenanceEventRepository maintenanceEventRepository;
    private final VehicleRepository vehicleRepository;
    private final HouseholdRepository householdRepository;
    private final Clock clock;

    public MaintenanceEventService(
            MaintenanceEventRepository maintenanceEventRepository,
            VehicleRepository vehicleRepository,
            HouseholdRepository householdRepository,
            Clock clock
    ) {
        this.maintenanceEventRepository = maintenanceEventRepository;
        this.vehicleRepository = vehicleRepository;
        this.householdRepository = householdRepository;
        this.clock = clock;
    }

    public MaintenanceEventResponse createMaintenanceEvent(UUID householdId, UUID vehicleId, MaintenanceEventRequest request) {
        Vehicle vehicle = findVehicle(householdId, vehicleId);
        Instant now = Instant.now(clock);

        MaintenanceEvent maintenanceEvent = new MaintenanceEvent(
                UUID.randomUUID(),
                vehicle,
                request.serviceDate(),
                normalizeRequiredText(request.description(), "description is required"),
                request.mileage(),
                request.cost(),
                normalizeOptionalText(request.notes()),
                now,
                now
        );

        return MaintenanceEventResponse.from(maintenanceEventRepository.save(maintenanceEvent));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceEventResponse> getMaintenanceEvents(UUID householdId, UUID vehicleId) {
        findVehicle(householdId, vehicleId);
        return maintenanceEventRepository.findByVehicle_IdOrderByServiceDateDescCreatedAtDesc(vehicleId)
                .stream()
                .map(MaintenanceEventResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public MaintenanceEventResponse getMaintenanceEvent(UUID householdId, UUID vehicleId, UUID eventId) {
        findVehicle(householdId, vehicleId);
        return MaintenanceEventResponse.from(findMaintenanceEvent(vehicleId, eventId));
    }

    public MaintenanceEventResponse updateMaintenanceEvent(
            UUID householdId,
            UUID vehicleId,
            UUID eventId,
            MaintenanceEventRequest request
    ) {
        Vehicle vehicle = findVehicle(householdId, vehicleId);
        MaintenanceEvent existingMaintenanceEvent = findMaintenanceEvent(vehicleId, eventId);

        MaintenanceEvent updatedMaintenanceEvent = new MaintenanceEvent(
                existingMaintenanceEvent.getId(),
                vehicle,
                request.serviceDate(),
                normalizeRequiredText(request.description(), "description is required"),
                request.mileage(),
                request.cost(),
                normalizeOptionalText(request.notes()),
                existingMaintenanceEvent.getCreatedAt(),
                Instant.now(clock)
        );

        return MaintenanceEventResponse.from(maintenanceEventRepository.save(updatedMaintenanceEvent));
    }

    public void deleteMaintenanceEvent(UUID householdId, UUID vehicleId, UUID eventId) {
        findVehicle(householdId, vehicleId);
        MaintenanceEvent existingMaintenanceEvent = findMaintenanceEvent(vehicleId, eventId);
        maintenanceEventRepository.delete(existingMaintenanceEvent);
    }

    private Vehicle findVehicle(UUID householdId, UUID vehicleId) {
        verifyHouseholdExists(householdId);
        return vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    private MaintenanceEvent findMaintenanceEvent(UUID vehicleId, UUID eventId) {
        return maintenanceEventRepository.findByIdAndVehicle_Id(eventId, vehicleId)
                .orElseThrow(() -> new MaintenanceEventNotFoundException(eventId));
    }

    private void verifyHouseholdExists(UUID householdId) {
        householdRepository.findById(householdId)
                .map(Household::getId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null) {
            throw new InvalidMaintenanceEventRequestException(message);
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new InvalidMaintenanceEventRequestException(message);
        }

        return trimmedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}
