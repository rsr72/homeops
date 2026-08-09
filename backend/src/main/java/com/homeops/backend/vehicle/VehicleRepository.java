package com.homeops.backend.vehicle;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

    List<Vehicle> findByHousehold_IdOrderByCreatedAtAsc(UUID householdId);

    Optional<Vehicle> findByIdAndHousehold_Id(UUID id, UUID householdId);
}