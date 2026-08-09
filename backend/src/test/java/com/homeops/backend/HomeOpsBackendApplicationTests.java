package com.homeops.backend;

import com.homeops.backend.household.HouseholdRepository;
import com.homeops.backend.maintenanceevent.MaintenanceEventRepository;
import com.homeops.backend.vehicle.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class HomeOpsBackendApplicationTests {

    @MockBean
    private HouseholdRepository householdRepository;

    @MockBean
    private VehicleRepository vehicleRepository;

    @MockBean
    private MaintenanceEventRepository maintenanceEventRepository;

    @Test
    void contextLoads() {
    }
}
