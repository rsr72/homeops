package com.homeops.backend.vehicle;

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

@WebMvcTest(VehicleController.class)
@Import(GlobalExceptionHandler.class)
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VehicleService vehicleService;

    @Test
    void createVehicleReturnsCreatedVehicle() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        given(vehicleService.createVehicle(eq(householdId), any(VehicleRequest.class)))
                .willReturn(vehicleResponse(vehicleId, householdId, "Honda", "Civic", 2020, "1HGCM82633A004352"));

        mockMvc.perform(post("/api/households/{householdId}/vehicles", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Honda",
                                  "model": "Civic",
                                  "year": 2020,
                                  "vin": "1hgcm82633a004352",
                                  "notes": "Primary car",
                                  "purchaseDate": "2024-01-15",
                                  "purchaseCost": 38999.95,
                                  "currentMileage": 9000
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.householdId").value(householdId.toString()))
                .andExpect(jsonPath("$.vin").value("1HGCM82633A004352"))
                .andExpect(jsonPath("$.make").value("Honda"))
                .andExpect(jsonPath("$.model").value("Civic"));
    }

    @Test
    void getVehiclesReturnsSavedVehicles() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID firstId = UUID.fromString("22222222-2222-2222-2222-222222222222");
        UUID secondId = UUID.fromString("33333333-3333-3333-3333-333333333333");

        given(vehicleService.getVehicles(householdId)).willReturn(List.of(
                vehicleResponse(firstId, householdId, "Toyota", "Camry", 2021, null),
                vehicleResponse(secondId, householdId, "Ford", "F-150", 2019, null)
        ));

        mockMvc.perform(get("/api/households/{householdId}/vehicles", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$[0].householdId").value(householdId.toString()))
                .andExpect(jsonPath("$[1].id").value(secondId.toString()));
    }

    @Test
    void getVehicleReturnsSavedVehicleById() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        given(vehicleService.getVehicle(householdId, vehicleId))
                .willReturn(vehicleResponse(vehicleId, householdId, "Subaru", "Outback", 2022, null));

        mockMvc.perform(get("/api/households/{householdId}/vehicles/{vehicleId}", householdId, vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.householdId").value(householdId.toString()))
                .andExpect(jsonPath("$.make").value("Subaru"));
    }

    @Test
    void updateVehicleReturnsUpdatedVehicle() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        given(vehicleService.updateVehicle(eq(householdId), eq(vehicleId), any(VehicleRequest.class)))
                .willReturn(vehicleResponse(vehicleId, householdId, "Mazda", "CX-50", 2023, "JM3KKDHC0R1100001"));

        mockMvc.perform(put("/api/households/{householdId}/vehicles/{vehicleId}", householdId, vehicleId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "make": "Mazda",
                                  "model": "CX-50",
                                  "year": 2023,
                                  "vin": "jm3kkdhc0r1100001",
                                  "notes": "Updated vehicle",
                                  "purchaseDate": "2024-01-15",
                                  "purchaseCost": 38999.95,
                                  "currentMileage": 9000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId.toString()))
                .andExpect(jsonPath("$.vin").value("JM3KKDHC0R1100001"));
    }

    @Test
    void deleteVehicleReturnsNoContent() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID vehicleId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        doNothing().when(vehicleService).deleteVehicle(householdId, vehicleId);

        mockMvc.perform(delete("/api/households/{householdId}/vehicles/{vehicleId}", householdId, vehicleId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createVehicleRejectsMissingRequiredFields() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(post("/api/households/{householdId}/vehicles", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "model": "Civic",
                                  "year": 2020
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)))
                .andExpect(jsonPath("$.validationErrors[0].field").value("make"));
    }

    @Test
    void createVehicleRejectsMalformedJson() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(post("/api/households/{householdId}/vehicles", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"make\":\"Honda\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON"));
    }

    @Test
    void getVehicleRejectsInvalidUuidPathVariables() throws Exception {
        mockMvc.perform(get("/api/households/{householdId}/vehicles/{vehicleId}", "not-a-uuid", "also-not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"));
    }

    private VehicleResponse vehicleResponse(UUID vehicleId, UUID householdId, String make, String model, int year, String vin) {
        Instant createdAt = Instant.parse("2026-08-09T12:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-09T12:00:00Z");
        return new VehicleResponse(
                vehicleId,
                householdId,
                make,
                model,
                year,
                vin,
                "Vehicle notes",
                LocalDate.of(2024, 1, 15),
                new BigDecimal("38999.95"),
                9000,
                createdAt,
                updatedAt
        );
    }
}