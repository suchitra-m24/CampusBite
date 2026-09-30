package com.campusbite.order;

import com.campusbite.kitchenload.KitchenLoadService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Runs periodically: if the kitchen's current backlog alone already exceeds
 * the time remaining before an order's promised pickup window ends, that
 * promise is realistically broken -- flag it once and let the student know,
 * rather than leaving them waiting with no signal.
 */
@Component
public class OrderDelayScheduler {

    private static final List<OrderStatus> WATCHED_STATUSES =
            List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED, OrderStatus.PREPARING);

    /** Extra buffer (minutes) before we call it "late" -- avoids flagging orders that are only marginally tight. */
    private static final int GRACE_MINUTES = 5;

    private final OrderRepository orderRepository;
    private final KitchenLoadService kitchenLoadService;
    private final OrderService orderService;

    public OrderDelayScheduler(OrderRepository orderRepository,
                                KitchenLoadService kitchenLoadService,
                                OrderService orderService) {
        this.orderRepository = orderRepository;
        this.kitchenLoadService = kitchenLoadService;
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 180000) // every 3 minutes
    public void checkForDelays() {
        int backlogMinutes = kitchenLoadService.getCurrentBacklogMinutes();
        Instant now = Instant.now();

        List<Order> active = orderRepository.findByStatusInOrderByPickupSlotStartAsc(WATCHED_STATUSES);

        for (Order order : active) {
            if (order.isDelayNotified()) continue;

            long minutesUntilSlotEnd = ChronoUnit.MINUTES.between(now, order.getPickupSlotEnd());

            if (backlogMinutes > minutesUntilSlotEnd + GRACE_MINUTES) {
                try {
                    orderService.markDelayNotified(order.getId());
                } catch (RuntimeException ex) {
                    org.slf4j.LoggerFactory.getLogger(OrderDelayScheduler.class)
                            .warn("Failed to mark order {} as delayed", order.getId(), ex);
                }
            }
        }
    }
}
