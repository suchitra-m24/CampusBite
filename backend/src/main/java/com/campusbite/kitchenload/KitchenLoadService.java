package com.campusbite.kitchenload;

import com.campusbite.common.ws.BroadcastService;
import com.campusbite.order.Order;
import com.campusbite.order.OrderRepository;
import com.campusbite.order.OrderStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Computes "how busy is the canteen right now" -- both as a raw number of
 * backlog minutes (used by SlotAllocationService to avoid over-promising
 * pickup times) and as the friendly LOW/MODERATE/BUSY/VERY_BUSY crowd meter
 * students see before ordering, in the spirit of Google's popular-times.
 */
@Service
public class KitchenLoadService {

    private static final List<OrderStatus> ACTIVE_KITCHEN_STATUSES =
            List.of(OrderStatus.CONFIRMED, OrderStatus.PREPARING);

    private final OrderRepository orderRepository;
    private final KitchenLoadSnapshotRepository snapshotRepository;
    private final BroadcastService broadcastService;
    private final int comfortableConcurrentCapacity;

    private volatile KitchenLoadDto latest;

    public KitchenLoadService(OrderRepository orderRepository,
                               KitchenLoadSnapshotRepository snapshotRepository,
                               BroadcastService broadcastService,
                               @Value("${campusbite.kitchen-load.comfortable-concurrent-capacity:15}") int comfortableConcurrentCapacity) {
        this.orderRepository = orderRepository;
        this.snapshotRepository = snapshotRepository;
        this.broadcastService = broadcastService;
        this.comfortableConcurrentCapacity = comfortableConcurrentCapacity;
        this.latest = new KitchenLoadDto(0, 0, 0, "LOW",
                "Kitchen load is initializing.", Instant.now());
    }

    /** Remaining committed prep work, in minutes, across everything the kitchen currently has in hand. */
    public int getCurrentBacklogMinutes() {
        List<Order> active = orderRepository.findByStatusInOrderByPickupSlotStartAsc(ACTIVE_KITCHEN_STATUSES);
        Instant now = Instant.now();
        int backlog = 0;
        for (Order o : active) {
            Instant startedAt = o.getPreparingAt() != null ? o.getPreparingAt() : o.getConfirmedAt();
            if (startedAt == null) {
                startedAt = o.getPlacedAt();
            }
            long elapsed = ChronoUnit.MINUTES.between(startedAt, now);
            int remaining = (int) Math.max(0, o.getEstimatedPrepMinutes() - elapsed);
            backlog += remaining;
        }
        return backlog;
    }

    public KitchenLoadDto getLatest() {
        return latest;
    }

    /** Re-reads the current order queue, stores a snapshot, and pushes it live. Called by the scheduler. */
    public KitchenLoadDto recalculate() {
        List<Order> active = orderRepository.findByStatusInOrderByPickupSlotStartAsc(ACTIVE_KITCHEN_STATUSES);
        int activeCount = active.size();
        int backlogMinutes = getCurrentBacklogMinutes();

        int percentage = comfortableConcurrentCapacity == 0
                ? 0
                : Math.min(100, (int) Math.round((activeCount * 100.0) / comfortableConcurrentCapacity));

        String level;
        String message;
        if (percentage < 30) {
            level = "LOW";
            message = "Kitchen is quiet -- orders are going out fast.";
        } else if (percentage < 60) {
            level = "MODERATE";
            message = "Steady flow. Pickup times are on schedule.";
        } else if (percentage < 85) {
            level = "BUSY";
            message = "Getting busy -- slot times are stretching a little.";
        } else {
            level = "VERY_BUSY";
            message = "Peak rush. Expect slightly longer pickup windows.";
        }

        KitchenLoadDto dto = new KitchenLoadDto(activeCount, backlogMinutes, percentage, level, message, Instant.now());
        this.latest = dto;

        snapshotRepository.save(new KitchenLoadSnapshot(activeCount, backlogMinutes, percentage, level));
        broadcastService.broadcastKitchenLoad(dto);
        return dto;
    }
}
