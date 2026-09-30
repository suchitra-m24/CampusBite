package com.campusbite.slot;

import com.campusbite.canteen.CanteenService;
import com.campusbite.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlotAllocationServiceTest {

    @Mock SlotCapacityRepository capacityRepository;
    @Mock SlotReservationService reservationService;
    @Mock CanteenService canteenService;

    @Test
    void selectsFirstWindowThatCanBeReserved() {
        SlotAllocationService service = new SlotAllocationService(
                capacityRepository, reservationService, 10, 15, canteenService, "Asia/Kolkata");

        AtomicInteger attempts = new AtomicInteger();
        when(canteenService.nextAllowedWindowStart(any(Instant.class), eq(10)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(capacityRepository.findByWindowStartMinuteOfDay(anyInt())).thenReturn(Optional.empty());
        when(reservationService.tryReserve(any(Instant.class), eq(15)))
                .thenAnswer(inv -> attempts.incrementAndGet() == 2);
        when(reservationService.getReservedCount(any(Instant.class))).thenReturn(3);

        var result = service.allocate(8, 0);

        assertThat(result.ordersAhead()).isEqualTo(2);
        assertThat(result.slotEnd()).isEqualTo(result.slotStart().plusSeconds(600));
        assertThat(attempts.get()).isEqualTo(2);
    }

    @Test
    void failsInsteadOfReturningAnUnreservedFallbackSlot() {
        SlotAllocationService service = new SlotAllocationService(
                capacityRepository, reservationService, 10, 15, canteenService, "Asia/Kolkata");

        when(canteenService.nextAllowedWindowStart(any(Instant.class), eq(10)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(capacityRepository.findByWindowStartMinuteOfDay(anyInt())).thenReturn(Optional.empty());
        when(reservationService.tryReserve(any(Instant.class), eq(15))).thenReturn(false);

        assertThatThrownBy(() -> service.allocate(8, 0))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No pickup slot is available");
    }
}
