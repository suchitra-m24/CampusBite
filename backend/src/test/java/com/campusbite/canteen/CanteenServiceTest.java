package com.campusbite.canteen;

import com.campusbite.common.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CanteenServiceTest {

    @Mock CanteenSettingsRepository settingsRepository;
    @Mock CanteenClosureRepository closureRepository;

    private CanteenService service;
    private final ZoneId zone = ZoneId.of("Asia/Kolkata");

    @BeforeEach
    void setUp() {
        CanteenSettings settings = new CanteenSettings();
        settings.setCanteenName("Campus Main Canteen");
        settings.setOpeningTime(LocalTime.of(8, 0));
        settings.setClosingTime(LocalTime.of(18, 0));
        settings.setOrderingEnabled(true);
        org.mockito.Mockito.lenient().when(settingsRepository.findTopByOrderByIdAsc()).thenReturn(java.util.Optional.of(settings));
        org.mockito.Mockito.lenient().when(closureRepository.existsByClosureDate(org.mockito.ArgumentMatchers.any(LocalDate.class))).thenReturn(false);
        service = new CanteenService(settingsRepository, closureRepository, zone.getId());
    }

    @Test
    void rejectsPickupWindowOutsideOperatingHours() {
        Instant start = LocalDate.of(2026, 9, 29).atTime(17, 55).atZone(zone).toInstant();
        Instant end = start.plusSeconds(10 * 60L);

        assertThat(service.isPickupWindowAllowed(start, end)).isFalse();
    }

    @Test
    void movesEarlyCandidateToOpeningTime() {
        Instant candidate = LocalDate.of(2026, 9, 29).atTime(7, 20).atZone(zone).toInstant();
        Instant expected = LocalDate.of(2026, 9, 29).atTime(8, 0).atZone(zone).toInstant();

        assertThat(service.nextAllowedWindowStart(candidate, 10)).isEqualTo(expected);
    }

    @Test
    void skipsClosedDay() {
        LocalDate closed = LocalDate.of(2026, 9, 29);
        when(closureRepository.existsByClosureDate(closed)).thenReturn(true);

        Instant candidate = closed.atTime(9, 0).atZone(zone).toInstant();
        Instant expected = closed.plusDays(1).atTime(8, 0).atZone(zone).toInstant();

        assertThat(service.nextAllowedWindowStart(candidate, 10)).isEqualTo(expected);
    }

    @Test
    void rejectsInvalidOperatingHours() {
        var request = new com.campusbite.canteen.dto.CanteenSettingsRequest(
                "Campus Main Canteen", "18:00", "08:00", true);

        assertThatThrownBy(() -> service.updateSettings(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Opening time must be before closing time");
    }
}
