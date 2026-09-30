package com.campusbite.slot;

import com.campusbite.canteen.CanteenService;
import com.campusbite.order.OrderRepository;
import com.campusbite.slot.dto.SlotAllocationResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

/**
 * The core mechanism behind CampusBite's "skip the line" promise.
 *
 * Rather than letting a student freely pick any pickup time (which is what
 * lets everyone pile into 12:30 and defeats the whole point), this service
 * walks forward through fixed-size time windows starting from the earliest
 * moment the kitchen could realistically have the order ready, and assigns
 * the FIRST window that still has room -- "room" being read from real
 * order counts already committed to that window, checked against an
 * admin-configurable capacity for that time of day.
 */
@Service
public class SlotAllocationService {

    private final SlotCapacityRepository slotCapacityRepository;
    private final SlotReservationService slotReservationService;
    private final int windowMinutes;
    private final int defaultCapacityPerWindow;
    private final CanteenService canteenService;
    private final ZoneId campusZone;

    public SlotAllocationService(SlotCapacityRepository slotCapacityRepository,
                                 SlotReservationService slotReservationService,
                                 @Value("${campusbite.slot.default-window-minutes}") int windowMinutes,
                                 @Value("${campusbite.slot.default-capacity-per-window}") int defaultCapacityPerWindow,
                                 CanteenService canteenService,
                                 @Value("${campusbite.time-zone:Asia/Kolkata}") String timeZone) {
        this.slotCapacityRepository = slotCapacityRepository;
        this.slotReservationService = slotReservationService;
        this.windowMinutes = windowMinutes;
        this.defaultCapacityPerWindow = defaultCapacityPerWindow;
        this.canteenService = canteenService;
        this.campusZone = ZoneId.of(timeZone);
    }

    /**
     * @param estimatedPrepMinutes prep time predicted for this specific cart
     *                              (see order.PrepTimePredictionService)
     * @param currentBacklogMinutes how many minutes of already-committed prep
     *                              work the kitchen is carrying right now
     *                              (see kitchenload.KitchenLoadService)
     */
    @Transactional
    public SlotAllocationResult allocate(int estimatedPrepMinutes, int currentBacklogMinutes) {
        ZoneId zone = campusZone;

        // Earliest moment the kitchen could plausibly start + finish this
        // order, given what's already queued ahead of it.
        int leadMinutes = Math.max(estimatedPrepMinutes, currentBacklogMinutes + estimatedPrepMinutes / 2);
        Instant earliestPossible = Instant.now().plus(leadMinutes, ChronoUnit.MINUTES);

        Instant windowStart = roundUpToWindow(earliestPossible, zone);

        // Safety cap: never search more than a day of windows ahead.
        int maxIterations = (24 * 60) / windowMinutes;
        for (int i = 0; i < maxIterations; i++) {
            windowStart = canteenService.nextAllowedWindowStart(windowStart, windowMinutes);
            int capacity = capacityFor(windowStart, zone);

            // Atomic claim -- this is the fix for the overbooking race: two
            // concurrent requests hitting the same nearly-full window can no
            // longer both succeed, since the increment-if-room happens as one
            // database-level conditional UPDATE, not a separate read then write.
            if (slotReservationService.tryReserve(windowStart, capacity)) {
                Instant windowEnd = windowStart.plus(windowMinutes, ChronoUnit.MINUTES);
                int ordersAhead = slotReservationService.getReservedCount(windowStart) - 1;
                return new SlotAllocationResult(windowStart, windowEnd, ordersAhead, estimatedPrepMinutes);
            }
            windowStart = windowStart.plus(windowMinutes, ChronoUnit.MINUTES);
        }

        throw new com.campusbite.common.exception.BadRequestException(
                "No pickup slot is available within the next 24 hours. Please try again later.");
    }

    private int capacityFor(Instant windowStart, ZoneId zone) {
        ZonedDateTime zdt = windowStart.atZone(zone);
        int minuteOfDay = zdt.getHour() * 60 + zdt.getMinute();
        return slotCapacityRepository.findByWindowStartMinuteOfDay(minuteOfDay)
                .map(SlotCapacity::getMaxOrders)
                .orElse(defaultCapacityPerWindow);
    }

    private Instant roundUpToWindow(Instant instant, ZoneId zone) {
        ZonedDateTime zdt = instant.atZone(zone).withSecond(0).withNano(0);
        int minute = zdt.getMinute();
        int remainder = minute % windowMinutes;
        if (remainder != 0) {
            zdt = zdt.plusMinutes(windowMinutes - remainder);
        }
        return zdt.toInstant();
    }
}
