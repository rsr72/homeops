package com.homeops.backend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import com.homeops.backend.household.HouseholdRepository;

@SpringBootTest
class HomeOpsBackendApplicationTests {

    @MockBean
    private HouseholdRepository householdRepository;

    @Test
    void contextLoads() {
    }
}
