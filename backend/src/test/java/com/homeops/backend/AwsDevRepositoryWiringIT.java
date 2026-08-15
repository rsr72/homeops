package com.homeops.backend;

import com.homeops.backend.household.HouseholdRepository;
import com.homeops.backend.maintenanceevent.MaintenanceEventRepository;
import com.homeops.backend.vehicle.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("aws-dev")
@Testcontainers
class AwsDevRepositoryWiringIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("homeops")
            .withUsername("homeops")
            .withPassword("homeops");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("APP_DB_HOST", postgres::getHost);
        registry.add("APP_DB_PORT", () -> postgres.getMappedPort(5432).toString());
        registry.add("APP_DB_NAME", postgres::getDatabaseName);
        registry.add("APP_DB_USER", postgres::getUsername);
        registry.add("APP_DB_PASSWORD", postgres::getPassword);
    }

    @Autowired
    private HouseholdRepository householdRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private MaintenanceEventRepository maintenanceEventRepository;

    @Test
    void awsDevProfileCreatesJpaRepositories() {
        assertThat(householdRepository).isNotNull();
        assertThat(vehicleRepository).isNotNull();
        assertThat(maintenanceEventRepository).isNotNull();
    }
}
