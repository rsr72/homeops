package com.homeops.backend.maintenanceevent;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events")
public class MaintenanceEventController {

    private final MaintenanceEventService maintenanceEventService;

    public MaintenanceEventController(MaintenanceEventService maintenanceEventService) {
        this.maintenanceEventService = maintenanceEventService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaintenanceEventResponse createMaintenanceEvent(
            @PathVariable UUID householdId,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody MaintenanceEventRequest request
    ) {
        return maintenanceEventService.createMaintenanceEvent(householdId, vehicleId, request);
    }

    @GetMapping
    public List<MaintenanceEventResponse> getMaintenanceEvents(@PathVariable UUID householdId, @PathVariable UUID vehicleId) {
        return maintenanceEventService.getMaintenanceEvents(householdId, vehicleId);
    }

    @GetMapping("/{eventId}")
    public MaintenanceEventResponse getMaintenanceEvent(
            @PathVariable UUID householdId,
            @PathVariable UUID vehicleId,
            @PathVariable UUID eventId
    ) {
        return maintenanceEventService.getMaintenanceEvent(householdId, vehicleId, eventId);
    }

    @PutMapping("/{eventId}")
    public MaintenanceEventResponse updateMaintenanceEvent(
            @PathVariable UUID householdId,
            @PathVariable UUID vehicleId,
            @PathVariable UUID eventId,
            @Valid @RequestBody MaintenanceEventRequest request
    ) {
        return maintenanceEventService.updateMaintenanceEvent(householdId, vehicleId, eventId, request);
    }

    @DeleteMapping("/{eventId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMaintenanceEvent(
            @PathVariable UUID householdId,
            @PathVariable UUID vehicleId,
            @PathVariable UUID eventId
    ) {
        maintenanceEventService.deleteMaintenanceEvent(householdId, vehicleId, eventId);
    }
}
