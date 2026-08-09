package com.homeops.backend.asset;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AssetControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InMemoryAssetRepository assetRepository;

  @MockBean
  private com.homeops.backend.household.HouseholdRepository householdRepository;

    @BeforeEach
    void clearRepository() {
        assetRepository.clear();
    }

    @Test
    void createAssetReturnsCreatedVehicle() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclePayload("Honda", "Civic", 2020, "1HGCM82633A004352", 34567)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isString())
                .andExpect(jsonPath("$.assetType").value("VEHICLE"))
                .andExpect(jsonPath("$.vehicle.make").value("Honda"))
                .andExpect(jsonPath("$.vehicle.model").value("Civic"))
                .andExpect(jsonPath("$.vehicle.year").value(2020))
                .andExpect(jsonPath("$.vehicle.vin").value("1HGCM82633A004352"))
                .andExpect(jsonPath("$.vehicle.currentMileage").value(34567))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.updatedAt").isString());
    }

    @Test
    void getAssetsReturnsSavedAssets() throws Exception {
        createAsset("Toyota", "Camry", 2021, null, 12345);
        createAsset("Ford", "F-150", 2019, "1FTFW1E50KFC12345", 67890);

        mockMvc.perform(get("/api/assets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].vehicle.make").value("Toyota"))
                .andExpect(jsonPath("$[1].vehicle.make").value("Ford"));
    }

    @Test
    void getAssetReturnsSavedAssetById() throws Exception {
        String assetId = createAsset("Subaru", "Outback", 2022, null, 18000);

        mockMvc.perform(get("/api/assets/{assetId}", assetId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetId))
                .andExpect(jsonPath("$.vehicle.make").value("Subaru"))
                .andExpect(jsonPath("$.vehicle.model").value("Outback"));
    }

    @Test
    void updateAssetReplacesStoredAsset() throws Exception {
        String assetId = createAsset("Mazda", "CX-5", 2018, null, 25000);

        mockMvc.perform(put("/api/assets/{assetId}", assetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"assetType\": \"VEHICLE\",
                                  \"notes\": \"Updated vehicle\",
                                  \"vehicle\": {
                                    \"make\": \"Mazda\",
                                    \"model\": \"CX-50\",
                                    \"year\": 2023,
                                    \"vin\": \"JM3KKDHC0R1100001\",
                                    \"purchaseDate\": \"2024-01-15\",
                                    \"purchaseCost\": 38999.95,
                                    \"currentMileage\": 9000
                                  }
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(assetId))
                .andExpect(jsonPath("$.notes").value("Updated vehicle"))
                .andExpect(jsonPath("$.vehicle.model").value("CX-50"))
                .andExpect(jsonPath("$.vehicle.year").value(2023))
                .andExpect(jsonPath("$.vehicle.vin").value("JM3KKDHC0R1100001"))
                .andExpect(jsonPath("$.vehicle.purchaseDate").value("2024-01-15"))
                .andExpect(jsonPath("$.vehicle.purchaseCost").value(38999.95))
                .andExpect(jsonPath("$.vehicle.currentMileage").value(9000));
    }

    @Test
    void deleteAssetRemovesStoredAsset() throws Exception {
        String assetId = createAsset("Tesla", "Model 3", 2021, null, 22000);

        mockMvc.perform(delete("/api/assets/{assetId}", assetId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/assets/{assetId}", assetId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void createAssetRejectsMissingRequiredVehicleFields() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"assetType\": \"VEHICLE\",
                                  \"vehicle\": {
                                    \"make\": \"\",
                                    \"year\": 2020
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors", hasSize(2)));
    }

    @Test
    void createAssetRejectsNegativeMileage() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclePayload("Jeep", "Wrangler", 2020, null, -1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("vehicle.currentMileage"));
    }

    @Test
    void createAssetRejectsMissingVehiclePayload() throws Exception {
        mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"assetType\": \"VEHICLE\"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.message").value("vehicle details are required for VEHICLE assets"));
    }

    @Test
    void getAssetReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(get("/api/assets/{assetId}", "missing-id"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Asset with id 'missing-id' was not found"));
    }

    private String createAsset(String make, String model, int year, String vin, Integer currentMileage) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/assets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(vehiclePayload(make, model, year, vin, currentMileage)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = new com.fasterxml.jackson.databind.ObjectMapper().readTree(result.getResponse().getContentAsString());
        return response.get("id").asText();
    }

    private String vehiclePayload(String make, String model, int year, String vin, Integer currentMileage) {
        StringBuilder payload = new StringBuilder("""
                {
                  "assetType": "VEHICLE",
                  \"vehicle\": {
                    "make": "%s",
                    "model": "%s",
                    "year": %d""".formatted(make, model, year));

        if (vin != null) {
            payload.append(",\n    \"vin\": \"%s\"".formatted(vin));
        }

        if (currentMileage != null) {
            payload.append(",\n    \"currentMileage\": %d".formatted(currentMileage));
        }

        payload.append("""

                  }
                }
                """);

        return payload.toString();
    }
}