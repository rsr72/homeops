package com.homeops.backend.vehicle;

import com.homeops.backend.household.Household;
import com.homeops.backend.household.HouseholdNotFoundException;
import com.homeops.backend.household.HouseholdRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private HouseholdRepository householdRepository;

    private VehicleService vehicleService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC);
        vehicleService = new VehicleService(vehicleRepository, householdRepository, clock);
    }

    @Test
    void createVehicleNormalizesVinAndSavesVehicle() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Household household = household(householdId);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.createVehicle(householdId, new VehicleRequest(
                "  Honda  ",
                "  Civic  ",
                2020,
                " 1hgcm82633a004352 ",
                "  Primary car  ",
                LocalDate.of(2024, 1, 15),
                new BigDecimal("38999.95"),
                9000
        ));

        assertThat(response.id()).isNotNull();
        assertThat(response.householdId()).isEqualTo(householdId);
        assertThat(response.make()).isEqualTo("Honda");
        assertThat(response.model()).isEqualTo("Civic");
        assertThat(response.vin()).isEqualTo("1HGCM82633A004352");
        assertThat(response.notes()).isEqualTo("Primary car");
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void getVehiclesReturnsHouseholdScopedVehicles() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Household household = household(householdId);
        Vehicle first = vehicle(UUID.fromString("22222222-2222-2222-2222-222222222222"), household, "Toyota", "Camry", 2021, null);
        Vehicle second = vehicle(UUID.fromString("33333333-3333-3333-3333-333333333333"), household, "Ford", "F-150", 2019, null);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByHousehold_IdOrderByCreatedAtAsc(householdId)).thenReturn(List.of(first, second));

        List<VehicleResponse> vehicles = vehicleService.getVehicles(householdId);

        assertThat(vehicles).hasSize(2);
        assertThat(vehicles.get(0).make()).isEqualTo("Toyota");
        assertThat(vehicles.get(1).make()).isEqualTo("Ford");
    }

    @Test
    void getVehicleReturnsSavedVehicle() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household, "Subaru", "Outback", 2022, null);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));

        VehicleResponse response = vehicleService.getVehicle(householdId, vehicleId);

        assertThat(response.id()).isEqualTo(vehicleId);
        assertThat(response.householdId()).isEqualTo(householdId);
        assertThat(response.make()).isEqualTo("Subaru");
    }

    @Test
    void getVehicleThrowsWhenAnotherHouseholdOwnsVehicle() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        Household household = household(householdId);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getVehicle(householdId, vehicleId))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle with id '44444444-4444-4444-4444-444444444444' was not found");
    }

    @Test
    void updateVehicleReplacesFieldsAndUppercasesVin() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Household household = household(householdId);
        Vehicle existing = vehicle(vehicleId, household, "Original", "Original", 2018, "ABC123");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateVehicle(householdId, vehicleId, new VehicleRequest(
                " Mazda ",
                " CX-50 ",
                2023,
                " jm3kkdhc0r1100001 ",
                " Updated vehicle ",
                LocalDate.of(2024, 1, 15),
                new BigDecimal("38999.95"),
                9000
        ));

        assertThat(response.id()).isEqualTo(vehicleId);
        assertThat(response.make()).isEqualTo("Mazda");
        assertThat(response.model()).isEqualTo("CX-50");
        assertThat(response.vin()).isEqualTo("JM3KKDHC0R1100001");
        assertThat(response.createdAt()).isEqualTo(existing.getCreatedAt());
    }

    @Test
    void deleteVehicleDeletesExistingVehicle() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Household household = household(householdId);
        Vehicle existing = vehicle(vehicleId, household, "Delete", "Me", 2021, null);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(existing));

        vehicleService.deleteVehicle(householdId, vehicleId);

        verify(vehicleRepository).delete(existing);
    }

    @Test
    void getVehicleThrowsWhenHouseholdMissing() {
        UUID householdId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID vehicleId = UUID.fromString("88888888-8888-8888-8888-888888888888");
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.getVehicle(householdId, vehicleId))
                .isInstanceOf(HouseholdNotFoundException.class)
                .hasMessage("Household with id '77777777-7777-7777-7777-777777777777' was not found");
    }

    @Test
    void createVehicleRejectsBlankMake() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        Household household = household(householdId);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        assertThatThrownBy(() -> vehicleService.createVehicle(householdId, new VehicleRequest(
                "   ",
                "Civic",
                2020,
                null,
                null,
                null,
                null,
                null
        )))
                .isInstanceOf(InvalidVehicleRequestException.class)
                .hasMessage("make is required");
    }

    private Household household(UUID householdId) {
        return new Household(
                householdId,
                "Household",
                null,
                Instant.parse("2026-08-09T11:00:00Z"),
                Instant.parse("2026-08-09T11:00:00Z")
        );
    }

    private Vehicle vehicle(UUID vehicleId, Household household, String make, String model, Integer year, String vin) {
        return new Vehicle(
                vehicleId,
                household,
                make,
                model,
                year,
                vin,
                "Notes",
                LocalDate.of(2024, 1, 15),
                new BigDecimal("38999.95"),
                9000,
                Instant.parse("2026-08-09T11:00:00Z"),
                Instant.parse("2026-08-09T11:00:00Z")
        );
    }
}