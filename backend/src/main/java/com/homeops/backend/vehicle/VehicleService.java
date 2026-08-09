package com.homeops.backend.vehicle;

import com.homeops.backend.household.Household;
import com.homeops.backend.household.HouseholdNotFoundException;
import com.homeops.backend.household.HouseholdRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final HouseholdRepository householdRepository;
    private final Clock clock;

    public VehicleService(VehicleRepository vehicleRepository, HouseholdRepository householdRepository, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.householdRepository = householdRepository;
        this.clock = clock;
    }

    public VehicleResponse createVehicle(UUID householdId, VehicleRequest request) {
        Household household = getHousehold(householdId);
        Instant now = Instant.now(clock);

        Vehicle vehicle = new Vehicle(
                UUID.randomUUID(),
                household,
                normalizeRequiredText(request.make(), "make is required"),
                normalizeRequiredText(request.model(), "model is required"),
                request.year(),
                normalizeOptionalVin(request.vin()),
                normalizeOptionalText(request.notes()),
                request.purchaseDate(),
                request.purchaseCost(),
                request.currentMileage(),
                now,
                now
        );

        return VehicleResponse.from(vehicleRepository.save(vehicle));
    }

    @Transactional(readOnly = true)
    public List<VehicleResponse> getVehicles(UUID householdId) {
        getHousehold(householdId);
        return vehicleRepository.findByHousehold_IdOrderByCreatedAtAsc(householdId).stream()
                .map(VehicleResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public VehicleResponse getVehicle(UUID householdId, UUID vehicleId) {
        getHousehold(householdId);
        return VehicleResponse.from(findVehicle(householdId, vehicleId));
    }

    public VehicleResponse updateVehicle(UUID householdId, UUID vehicleId, VehicleRequest request) {
        Household household = getHousehold(householdId);
        Vehicle existingVehicle = findVehicle(householdId, vehicleId);

        Vehicle updatedVehicle = new Vehicle(
                existingVehicle.getId(),
                household,
                normalizeRequiredText(request.make(), "make is required"),
                normalizeRequiredText(request.model(), "model is required"),
                request.year(),
                normalizeOptionalVin(request.vin()),
                normalizeOptionalText(request.notes()),
                request.purchaseDate(),
                request.purchaseCost(),
                request.currentMileage(),
                existingVehicle.getCreatedAt(),
                Instant.now(clock)
        );

        return VehicleResponse.from(vehicleRepository.save(updatedVehicle));
    }

    public void deleteVehicle(UUID householdId, UUID vehicleId) {
        getHousehold(householdId);
        Vehicle existingVehicle = findVehicle(householdId, vehicleId);
        vehicleRepository.delete(existingVehicle);
    }

    private Household getHousehold(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
    }

    private Vehicle findVehicle(UUID householdId, UUID vehicleId) {
        return vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)
                .orElseThrow(() -> new VehicleNotFoundException(vehicleId));
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null) {
            throw new InvalidVehicleRequestException(message);
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new InvalidVehicleRequestException(message);
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

    private String normalizeOptionalVin(String value) {
        String normalizedValue = normalizeOptionalText(value);
        return normalizedValue == null ? null : normalizedValue.toUpperCase(Locale.ROOT);
    }
}