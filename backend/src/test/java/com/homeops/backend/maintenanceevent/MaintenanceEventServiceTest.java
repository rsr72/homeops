package com.homeops.backend.maintenanceevent;

import com.homeops.backend.household.Household;
import com.homeops.backend.household.HouseholdNotFoundException;
import com.homeops.backend.household.HouseholdRepository;
import com.homeops.backend.vehicle.Vehicle;
import com.homeops.backend.vehicle.VehicleNotFoundException;
import com.homeops.backend.vehicle.VehicleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MaintenanceEventServiceTest {

    @Mock
    private MaintenanceEventRepository maintenanceEventRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private HouseholdRepository householdRepository;

    private MaintenanceEventService maintenanceEventService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC);
        maintenanceEventService = new MaintenanceEventService(maintenanceEventRepository, vehicleRepository, householdRepository, clock);
    }

    @Test
    void createMaintenanceEventNormalizesTextAndSavesEvent() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household);

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));
        when(maintenanceEventRepository.save(any(MaintenanceEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceEventResponse response = maintenanceEventService.createMaintenanceEvent(
                householdId,
                vehicleId,
                new MaintenanceEventRequest(
                        LocalDate.of(2026, 8, 1),
                        "  Oil change  ",
                        12345,
                        new BigDecimal("89.99"),
                        "  Changed filter too  "
                )
        );

        assertThat(response.id()).isNotNull();
        assertThat(response.householdId()).isEqualTo(householdId);
        assertThat(response.vehicleId()).isEqualTo(vehicleId);
        assertThat(response.description()).isEqualTo("Oil change");
        assertThat(response.notes()).isEqualTo("Changed filter too");
        verify(maintenanceEventRepository).save(any(MaintenanceEvent.class));
    }

    @Test
    void getMaintenanceEventsReturnsVehicleScopedEvents() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household);

        MaintenanceEvent newer = maintenanceEvent(
                UUID.fromString("33333333-3333-3333-3333-333333333333"),
                vehicle,
                LocalDate.of(2026, 8, 1),
                Instant.parse("2026-08-01T12:00:00Z")
        );
        MaintenanceEvent older = maintenanceEvent(
                UUID.fromString("44444444-4444-4444-4444-444444444444"),
                vehicle,
                LocalDate.of(2026, 7, 15),
                Instant.parse("2026-07-15T12:00:00Z")
        );

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));
        when(maintenanceEventRepository.findByVehicle_IdOrderByServiceDateDescCreatedAtDesc(vehicleId)).thenReturn(List.of(newer, older));

        List<MaintenanceEventResponse> events = maintenanceEventService.getMaintenanceEvents(householdId, vehicleId);

        assertThat(events).hasSize(2);
        assertThat(events.get(0).id()).isEqualTo(newer.getId());
        assertThat(events.get(1).id()).isEqualTo(older.getId());
    }

    @Test
    void getMaintenanceEventReturnsSavedEvent() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household);
        MaintenanceEvent event = maintenanceEvent(eventId, vehicle, LocalDate.of(2026, 8, 1), Instant.parse("2026-08-01T12:00:00Z"));

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));
        when(maintenanceEventRepository.findByIdAndVehicle_Id(eventId, vehicleId)).thenReturn(Optional.of(event));

        MaintenanceEventResponse response = maintenanceEventService.getMaintenanceEvent(householdId, vehicleId, eventId);

        assertThat(response.id()).isEqualTo(eventId);
        assertThat(response.vehicleId()).isEqualTo(vehicleId);
    }

    @Test
    void updateMaintenanceEventReplacesFieldsAndKeepsCreatedAt() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household);
        MaintenanceEvent existing = maintenanceEvent(eventId, vehicle, LocalDate.of(2026, 7, 1), Instant.parse("2026-07-01T12:00:00Z"));

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));
        when(maintenanceEventRepository.findByIdAndVehicle_Id(eventId, vehicleId)).thenReturn(Optional.of(existing));
        when(maintenanceEventRepository.save(any(MaintenanceEvent.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaintenanceEventResponse response = maintenanceEventService.updateMaintenanceEvent(
                householdId,
                vehicleId,
                eventId,
                new MaintenanceEventRequest(
                        LocalDate.of(2026, 8, 2),
                        "  Brake service  ",
                        13000,
                        new BigDecimal("210.50"),
                        "  Front pads replaced  "
                )
        );

        assertThat(response.id()).isEqualTo(eventId);
        assertThat(response.description()).isEqualTo("Brake service");
        assertThat(response.notes()).isEqualTo("Front pads replaced");
        assertThat(response.createdAt()).isEqualTo(existing.getCreatedAt());
        assertThat(response.updatedAt()).isEqualTo(Instant.parse("2026-08-09T12:00:00Z"));
    }

    @Test
    void deleteMaintenanceEventDeletesExistingEvent() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household);
        MaintenanceEvent existing = maintenanceEvent(eventId, vehicle, LocalDate.of(2026, 7, 1), Instant.parse("2026-07-01T12:00:00Z"));

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));
        when(maintenanceEventRepository.findByIdAndVehicle_Id(eventId, vehicleId)).thenReturn(Optional.of(existing));

        maintenanceEventService.deleteMaintenanceEvent(householdId, vehicleId, eventId);

        verify(maintenanceEventRepository).delete(existing);
    }

    @Test
    void getMaintenanceEventThrowsWhenEventIsOutsideVehicleScope() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household);

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));
        when(maintenanceEventRepository.findByIdAndVehicle_Id(eventId, vehicleId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceEventService.getMaintenanceEvent(householdId, vehicleId, eventId))
                .isInstanceOf(MaintenanceEventNotFoundException.class)
                .hasMessage("Maintenance event with id '33333333-3333-3333-3333-333333333333' was not found");
    }

    @Test
    void getMaintenanceEventsThrowsWhenVehicleIsOutsideHouseholdScope() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Household household = household(householdId);

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceEventService.getMaintenanceEvents(householdId, vehicleId))
                .isInstanceOf(VehicleNotFoundException.class)
                .hasMessage("Vehicle with id '22222222-2222-2222-2222-222222222222' was not found");
    }

    @Test
    void createMaintenanceEventRejectsBlankDescription() {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        Household household = household(householdId);
        Vehicle vehicle = vehicle(vehicleId, household);

        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));
        when(vehicleRepository.findByIdAndHousehold_Id(vehicleId, householdId)).thenReturn(Optional.of(vehicle));

        assertThatThrownBy(() -> maintenanceEventService.createMaintenanceEvent(
                householdId,
                vehicleId,
                new MaintenanceEventRequest(LocalDate.of(2026, 8, 1), "   ", 12345, new BigDecimal("45.00"), null)
        ))
                .isInstanceOf(InvalidMaintenanceEventRequestException.class)
                .hasMessage("description is required");
    }

    @Test
    void getMaintenanceEventThrowsWhenHouseholdMissing() {
        UUID householdId = UUID.fromString("77777777-7777-7777-7777-777777777777");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> maintenanceEventService.getMaintenanceEvent(householdId, vehicleId, eventId))
                .isInstanceOf(HouseholdNotFoundException.class)
                .hasMessage("Household with id '77777777-7777-7777-7777-777777777777' was not found");
    }

    private Household household(UUID id) {
        return new Household(
                id,
                "Household",
                "Notes",
                Instant.parse("2026-08-09T11:00:00Z"),
                Instant.parse("2026-08-09T11:00:00Z")
        );
    }

    private Vehicle vehicle(UUID id, Household household) {
        return new Vehicle(
                id,
                household,
                "Honda",
                "Civic",
                2022,
                "1HGCM82633A004352",
                "Vehicle notes",
                LocalDate.of(2024, 1, 15),
                new BigDecimal("28999.99"),
                12000,
                Instant.parse("2026-08-09T11:00:00Z"),
                Instant.parse("2026-08-09T11:00:00Z")
        );
    }

    private MaintenanceEvent maintenanceEvent(UUID id, Vehicle vehicle, LocalDate serviceDate, Instant createdAt) {
        return new MaintenanceEvent(
                id,
                vehicle,
                serviceDate,
                "Service description",
                12000,
                new BigDecimal("89.99"),
                "Event notes",
                createdAt,
                createdAt
        );
    }
}
