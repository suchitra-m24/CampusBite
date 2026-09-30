package com.campusbite.order;

import com.campusbite.canteen.CanteenService;
import org.springframework.scheduling.annotation.Scheduled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Closes stale kitchen tickets automatically. A PLACED/CONFIRMED order whose
 * pickup window has already ended must not remain on the kitchen queue forever.
 * The same rule also handles a holiday/maintenance closure that is added after
 * an order was already booked for that date.
 */
@Component
public class OrderExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpiryScheduler.class);

    private static final List<OrderStatus> EXPIRABLE =
            List.of(OrderStatus.PLACED, OrderStatus.CONFIRMED);

    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CanteenService canteenService;
    private final ZoneId campusZone;

    public OrderExpiryScheduler(OrderRepository orderRepository,
                                OrderService orderService,
                                CanteenService canteenService,
                                @org.springframework.beans.factory.annotation.Value("${campusbite.time-zone:Asia/Kolkata}") String timeZone) {
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.canteenService = canteenService;
        this.campusZone = ZoneId.of(timeZone);
    }

    @Scheduled(initialDelay = 10000, fixedRate = 60000)
    public void expireStaleOrders() {
        Instant now = Instant.now();
        for (Order order : orderRepository.findByStatusInOrderByPickupSlotStartAsc(EXPIRABLE)) {
            var slotDate = order.getPickupSlotStart().atZone(campusZone).toLocalDate();
            boolean slotEnded = !order.getPickupSlotEnd().isAfter(now);
            boolean closedDate = canteenService.isClosedOn(slotDate);
            boolean canteenWindowClosed = !canteenService.isWithinOperatingHours(
                    order.getPickupSlotStart(), order.getPickupSlotEnd());
            if (slotEnded || closedDate || canteenWindowClosed) {
                try {
                    String reason = (closedDate || canteenWindowClosed)
                            ? "Order cancelled because the canteen is closed or the selected pickup window is no longer operating."
                            : "Order expired because its pickup window has ended.";
                    orderService.expireOrder(order.getId(), reason);
                } catch (RuntimeException ex) {
                    // Isolate failures so one order cannot poison the batch, but never hide them.
                    log.warn("Failed to expire stale order {}", order.getId(), ex);
                }
            }
        }
    }
}
