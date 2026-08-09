package com.homeops.backend.household;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/households")
public class HouseholdController {

    private final HouseholdService householdService;

    public HouseholdController(HouseholdService householdService) {
        this.householdService = householdService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HouseholdResponse createHousehold(@Valid @RequestBody HouseholdRequest request) {
        return HouseholdResponse.from(householdService.createHousehold(request));
    }

    @GetMapping
    public List<HouseholdResponse> getHouseholds() {
        return householdService.getHouseholds().stream()
                .map(HouseholdResponse::from)
                .toList();
    }

    @GetMapping("/{householdId}")
    public HouseholdResponse getHousehold(@PathVariable UUID householdId) {
        return HouseholdResponse.from(householdService.getHousehold(householdId));
    }

    @PutMapping("/{householdId}")
    public HouseholdResponse updateHousehold(@PathVariable UUID householdId, @Valid @RequestBody HouseholdRequest request) {
        return HouseholdResponse.from(householdService.updateHousehold(householdId, request));
    }

    @DeleteMapping("/{householdId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteHousehold(@PathVariable UUID householdId) {
        householdService.deleteHousehold(householdId);
    }
}