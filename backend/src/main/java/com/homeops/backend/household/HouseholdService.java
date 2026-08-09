package com.homeops.backend.household;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class HouseholdService {

    private final HouseholdRepository householdRepository;
    private final Clock clock;

    public HouseholdService(HouseholdRepository householdRepository, Clock clock) {
        this.householdRepository = householdRepository;
        this.clock = clock;
    }

    public Household createHousehold(HouseholdRequest request) {
        String name = normalizeRequiredText(request.name(), "name is required");
        String notes = normalizeOptionalText(request.notes());
        Instant now = Instant.now(clock);

        Household household = new Household(UUID.randomUUID(), name, notes, now, now);
        return householdRepository.save(household);
    }

    @Transactional(readOnly = true)
    public List<Household> getHouseholds() {
        return householdRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt"));
    }

    @Transactional(readOnly = true)
    public Household getHousehold(UUID householdId) {
        return householdRepository.findById(householdId)
                .orElseThrow(() -> new HouseholdNotFoundException(householdId));
    }

    public Household updateHousehold(UUID householdId, HouseholdRequest request) {
        Household existingHousehold = getHousehold(householdId);
        String name = normalizeRequiredText(request.name(), "name is required");
        String notes = normalizeOptionalText(request.notes());

        Household updatedHousehold = new Household(
                existingHousehold.getId(),
                name,
                notes,
                existingHousehold.getCreatedAt(),
                Instant.now(clock)
        );

        return householdRepository.save(updatedHousehold);
    }

    public void deleteHousehold(UUID householdId) {
        Household existingHousehold = getHousehold(householdId);
        householdRepository.delete(existingHousehold);
    }

    private String normalizeRequiredText(String value, String message) {
        if (value == null) {
            throw new InvalidHouseholdRequestException(message);
        }

        String trimmedValue = value.trim();
        if (trimmedValue.isEmpty()) {
            throw new InvalidHouseholdRequestException(message);
        }

        return trimmedValue;
    }

    private String normalizeOptionalText(String value) {
        if (value == null) {
            return null;
        }

        String trimmedValue = value.trim();
        return trimmedValue.isEmpty() ? null : trimmedValue;
    }
}