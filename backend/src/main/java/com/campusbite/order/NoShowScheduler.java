package com.campusbite.order;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/** Marks READY orders as NO_SHOW only after their promised pickup window has ended plus grace. */
@Component
public class NoShowScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final int graceMinutes;

    public NoShowScheduler(OrderRepository orderRepository,
                           OrderService orderService,
                           @Value("${campusbite.order.no-show-grace-minutes}") int graceMinutes) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.graceMinutes = graceMinutes;
    }

    @Scheduled(fixedRate = 300000)
    public void detectNoShows() {
        Instant cutoff = Instant.now().minus(graceMinutes, ChronoUnit.MINUTES);
        List<Order> overdue = orderRepository.findByStatusAndPickupSlotEndBefore(OrderStatus.READY, cutoff);
        for (Order order : overdue) {
            try {
                orderService.markNoShow(order.getId());
            } catch (RuntimeException ex) {
                // Isolate one broken order from the rest of the scheduler batch.
                org.slf4j.LoggerFactory.getLogger(NoShowScheduler.class)
                        .warn("Failed to mark order {} as no-show", order.getId(), ex);
            }
        }
    }
}
