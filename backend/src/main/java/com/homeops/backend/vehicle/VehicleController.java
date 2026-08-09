package com.homeops.backend.vehicle;

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
@RequestMapping("/api/households/{householdId}/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleResponse createVehicle(@PathVariable UUID householdId, @Valid @RequestBody VehicleRequest request) {
        return vehicleService.createVehicle(householdId, request);
    }

    @GetMapping
    public List<VehicleResponse> getVehicles(@PathVariable UUID householdId) {
        return vehicleService.getVehicles(householdId);
    }

    @GetMapping("/{vehicleId}")
    public VehicleResponse getVehicle(@PathVariable UUID householdId, @PathVariable UUID vehicleId) {
        return vehicleService.getVehicle(householdId, vehicleId);
    }

    @PutMapping("/{vehicleId}")
    public VehicleResponse updateVehicle(
            @PathVariable UUID householdId,
            @PathVariable UUID vehicleId,
            @Valid @RequestBody VehicleRequest request
    ) {
        return vehicleService.updateVehicle(householdId, vehicleId, request);
    }

    @DeleteMapping("/{vehicleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVehicle(@PathVariable UUID householdId, @PathVariable UUID vehicleId) {
        vehicleService.deleteVehicle(householdId, vehicleId);
    }
}