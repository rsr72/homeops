package com.homeops.backend.household;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.Instant;
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
class HouseholdServiceTest {

    @Mock
    private HouseholdRepository householdRepository;

    private HouseholdService householdService;

    private Clock clock;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-08-09T12:00:00Z"), ZoneOffset.UTC);
        householdService = new HouseholdService(householdRepository, clock);
    }

    @Test
    void createHouseholdNormalizesAndSavesHousehold() {
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Household household = householdService.createHousehold(new HouseholdRequest("  Main Household  ", "  Important notes  "));

        assertThat(household.getId()).isNotNull();
        assertThat(household.getName()).isEqualTo("Main Household");
        assertThat(household.getNotes()).isEqualTo("Important notes");
        assertThat(household.getCreatedAt()).isEqualTo(Instant.parse("2026-08-09T12:00:00Z"));
        assertThat(household.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-09T12:00:00Z"));
        verify(householdRepository).save(any(Household.class));
    }

    @Test
    void getHouseholdsReturnsSortedHouseholds() {
        Household first = household("11111111-1111-1111-1111-111111111111", "First", null);
        Household second = household("22222222-2222-2222-2222-222222222222", "Second", "Notes");
        when(householdRepository.findAll(any(Sort.class))).thenReturn(List.of(first, second));

        List<Household> households = householdService.getHouseholds();

        assertThat(households).containsExactly(first, second);
    }

    @Test
    void getHouseholdReturnsSavedHousehold() {
        UUID householdId = UUID.fromString("33333333-3333-3333-3333-333333333333");
        Household household = household(householdId.toString(), "Household", null);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        Household result = householdService.getHousehold(householdId);

        assertThat(result).isEqualTo(household);
    }

    @Test
    void getHouseholdThrowsWhenMissing() {
        UUID householdId = UUID.fromString("44444444-4444-4444-4444-444444444444");
        when(householdRepository.findById(householdId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> householdService.getHousehold(householdId))
                .isInstanceOf(HouseholdNotFoundException.class)
                .hasMessage("Household with id '44444444-4444-4444-4444-444444444444' was not found");
    }

    @Test
    void updateHouseholdReplacesFieldsAndUpdatesTimestamp() {
        UUID householdId = UUID.fromString("55555555-5555-5555-5555-555555555555");
        Household existing = household(householdId.toString(), "Original", "Original notes");
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(existing));
        when(householdRepository.save(any(Household.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Household updated = householdService.updateHousehold(householdId, new HouseholdRequest(" Updated Household ", " Updated notes "));

        assertThat(updated.getId()).isEqualTo(householdId);
        assertThat(updated.getName()).isEqualTo("Updated Household");
        assertThat(updated.getNotes()).isEqualTo("Updated notes");
        assertThat(updated.getCreatedAt()).isEqualTo(existing.getCreatedAt());
        assertThat(updated.getUpdatedAt()).isEqualTo(Instant.parse("2026-08-09T12:00:00Z"));
    }

    @Test
    void deleteHouseholdDeletesExistingHousehold() {
        UUID householdId = UUID.fromString("66666666-6666-6666-6666-666666666666");
        Household household = household(householdId.toString(), "Delete Me", null);
        when(householdRepository.findById(householdId)).thenReturn(Optional.of(household));

        householdService.deleteHousehold(householdId);

        verify(householdRepository).delete(household);
    }

    @Test
    void createHouseholdRejectsBlankName() {
        assertThatThrownBy(() -> householdService.createHousehold(new HouseholdRequest("   ", null)))
                .isInstanceOf(InvalidHouseholdRequestException.class)
                .hasMessage("name is required");
    }

    private Household household(String id, String name, String notes) {
        return household(UUID.fromString(id), name, notes);
    }

    private Household household(UUID id, String name, String notes) {
        return new Household(
                id,
                name,
                notes,
                Instant.parse("2026-08-09T11:00:00Z"),
                Instant.parse("2026-08-09T11:00:00Z")
        );
    }
}