package com.homeops.backend.maintenanceevent;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MaintenanceEventRepository extends JpaRepository<MaintenanceEvent, UUID> {

    List<MaintenanceEvent> findByVehicle_IdOrderByServiceDateDescCreatedAtDesc(UUID vehicleId);

    Optional<MaintenanceEvent> findByIdAndVehicle_Id(UUID id, UUID vehicleId);
}
