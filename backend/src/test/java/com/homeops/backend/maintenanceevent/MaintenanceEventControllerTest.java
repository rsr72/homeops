package com.homeops.backend.maintenanceevent;

import com.homeops.backend.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MaintenanceEventController.class)
@Import(GlobalExceptionHandler.class)
class MaintenanceEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaintenanceEventService maintenanceEventService;

    @Test
    void createMaintenanceEventReturnsCreatedEvent() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        given(maintenanceEventService.createMaintenanceEvent(eq(householdId), eq(vehicleId), any(MaintenanceEventRequest.class)))
                .willReturn(maintenanceEventResponse(householdId, vehicleId, eventId, "Oil change"));

        mockMvc.perform(post("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events", householdId, vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceDate": "2026-08-01",
                                  "description": "Oil change",
                                  "mileage": 12345,
                                  "cost": 89.99,
                                  "notes": "Changed filter"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.householdId").value(householdId.toString()))
                .andExpect(jsonPath("$.vehicleId").value(vehicleId.toString()))
                .andExpect(jsonPath("$.description").value("Oil change"));
    }

    @Test
    void getMaintenanceEventsReturnsVehicleScopedEvents() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID firstId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        UUID secondId = UUID.fromString("44444444-4444-4444-4444-444444444444");

        given(maintenanceEventService.getMaintenanceEvents(householdId, vehicleId))
                .willReturn(List.of(
                        maintenanceEventResponse(householdId, vehicleId, firstId, "Brake service"),
                        maintenanceEventResponse(householdId, vehicleId, secondId, "Tire rotation")
                ));

        mockMvc.perform(get("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events", householdId, vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$[1].id").value(secondId.toString()));
    }

    @Test
    void getMaintenanceEventReturnsSingleEvent() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        given(maintenanceEventService.getMaintenanceEvent(householdId, vehicleId, eventId))
                .willReturn(maintenanceEventResponse(householdId, vehicleId, eventId, "Oil change"));

        mockMvc.perform(get("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events/{eventId}", householdId, vehicleId, eventId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()));
    }

    @Test
    void updateMaintenanceEventReturnsUpdatedEvent() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        given(maintenanceEventService.updateMaintenanceEvent(eq(householdId), eq(vehicleId), eq(eventId), any(MaintenanceEventRequest.class)))
                .willReturn(maintenanceEventResponse(householdId, vehicleId, eventId, "Updated service"));

        mockMvc.perform(put("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events/{eventId}", householdId, vehicleId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceDate": "2026-08-03",
                                  "description": "Updated service",
                                  "mileage": 13000,
                                  "cost": 120.50,
                                  "notes": "Updated notes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(eventId.toString()))
                .andExpect(jsonPath("$.description").value("Updated service"));
    }

    @Test
    void deleteMaintenanceEventReturnsNoContent() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        doNothing().when(maintenanceEventService).deleteMaintenanceEvent(householdId, vehicleId, eventId);

        mockMvc.perform(delete("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events/{eventId}", householdId, vehicleId, eventId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createMaintenanceEventRejectsMissingRequiredFields() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        mockMvc.perform(post("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events", householdId, vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notes": "Only notes"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors", hasSize(2)));
    }

    @Test
    void updateMaintenanceEventRejectsMalformedJson() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID eventId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        mockMvc.perform(put("/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events/{eventId}", householdId, vehicleId, eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"bad\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON"));
    }

    @Test
    void getMaintenanceEventRejectsInvalidUuidPathVariables() throws Exception {
        mockMvc.perform(get(
                        "/api/households/{householdId}/vehicles/{vehicleId}/maintenance-events/{eventId}",
                        "not-a-uuid",
                        "also-not-a-uuid",
                        "event-not-uuid"
                ))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    private MaintenanceEventResponse maintenanceEventResponse(
            UUID householdId,
            UUID vehicleId,
            UUID eventId,
            String description
    ) {
        Instant now = Instant.parse("2026-08-09T12:00:00Z");
        return new MaintenanceEventResponse(
                eventId,
                householdId,
                vehicleId,
                LocalDate.of(2026, 8, 1),
                description,
                12345,
                new BigDecimal("89.99"),
                "Event notes",
                now,
                now
        );
    }
}
