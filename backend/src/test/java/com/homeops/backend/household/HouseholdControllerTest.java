package com.homeops.backend.household;

import com.homeops.backend.api.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HouseholdController.class)
@Import(GlobalExceptionHandler.class)
class HouseholdControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private HouseholdService householdService;

    @Test
    void createHouseholdReturnsCreatedHousehold() throws Exception {
        UUID householdId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        HouseholdResponse response = householdResponse(householdId, "Main Household", "Important notes");

        given(householdService.createHousehold(any(HouseholdRequest.class))).willReturn(householdFromResponse(response));

        mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main Household",
                                  "notes": "Important notes"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(householdId.toString()))
                .andExpect(jsonPath("$.name").value("Main Household"))
                .andExpect(jsonPath("$.notes").value("Important notes"))
                .andExpect(jsonPath("$.createdAt").value("2026-08-09T12:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2026-08-09T12:00:00Z"));
    }

    @Test
    void getHouseholdsReturnsSavedHouseholds() throws Exception {
        UUID firstId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID secondId = UUID.fromString("22222222-2222-2222-2222-222222222222");

        given(householdService.getHouseholds()).willReturn(List.of(
                householdFromResponse(householdResponse(firstId, "Household One", null)),
                householdFromResponse(householdResponse(secondId, "Household Two", "Notes"))
        ));

        mockMvc.perform(get("/api/households"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(firstId.toString()))
                .andExpect(jsonPath("$[0].name").value("Household One"))
                .andExpect(jsonPath("$[1].id").value(secondId.toString()))
                .andExpect(jsonPath("$[1].notes").value("Notes"));
    }

    @Test
    void getHouseholdReturnsSavedHouseholdById() throws Exception {
        UUID householdId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        given(householdService.getHousehold(householdId)).willReturn(householdFromResponse(householdResponse(householdId, "Household", null)));

        mockMvc.perform(get("/api/households/{householdId}", householdId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(householdId.toString()))
                .andExpect(jsonPath("$.name").value("Household"));
    }

    @Test
    void updateHouseholdReturnsUpdatedHousehold() throws Exception {
        UUID householdId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        HouseholdResponse response = householdResponse(householdId, "Updated Household", "Updated notes");
        given(householdService.updateHousehold(eq(householdId), any(HouseholdRequest.class)))
                .willReturn(householdFromResponse(response));

        mockMvc.perform(put("/api/households/{householdId}", householdId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Updated Household",
                                  "notes": "Updated notes"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(householdId.toString()))
                .andExpect(jsonPath("$.name").value("Updated Household"))
                .andExpect(jsonPath("$.notes").value("Updated notes"));
    }

    @Test
    void deleteHouseholdReturnsNoContent() throws Exception {
        UUID householdId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        doNothing().when(householdService).deleteHousehold(householdId);

        mockMvc.perform(delete("/api/households/{householdId}", householdId))
                .andExpect(status().isNoContent());
    }

    @Test
    void createHouseholdRejectsMissingName() throws Exception {
        mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notes": "Missing name"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors", hasSize(1)))
                .andExpect(jsonPath("$.validationErrors[0].field").value("name"));
    }

    @Test
    void createHouseholdRejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/households")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Household\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_JSON"));
    }

    @Test
    void getHouseholdReturnsNotFoundForUnknownId() throws Exception {
        UUID householdId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        given(householdService.getHousehold(householdId)).willThrow(new HouseholdNotFoundException(householdId));

        mockMvc.perform(get("/api/households/{householdId}", householdId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Household with id '66666666-6666-6666-6666-666666666666' was not found"));
    }

    private HouseholdResponse householdResponse(UUID id, String name, String notes) {
        Instant createdAt = Instant.parse("2026-08-09T12:00:00Z");
        Instant updatedAt = Instant.parse("2026-08-09T12:00:00Z");
        return new HouseholdResponse(id, name, notes, createdAt, updatedAt);
    }

    private Household householdFromResponse(HouseholdResponse response) {
        return new Household(
                response.id(),
                response.name(),
                response.notes(),
                response.createdAt(),
                response.updatedAt()
        );
    }
}