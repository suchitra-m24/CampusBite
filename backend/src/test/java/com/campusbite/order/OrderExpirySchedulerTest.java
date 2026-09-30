package com.campusbite.order;

import com.campusbite.canteen.CanteenService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExpirySchedulerTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderService orderService;
    @Mock CanteenService canteenService;

    @Test
    void expiresPlacedOrderAfterPickupWindowEnds() {
        Order order = new Order();
        order.setId(42L);
        order.setStatus(OrderStatus.PLACED);
        order.setPickupSlotStart(Instant.now().minusSeconds(20 * 60L));
        order.setPickupSlotEnd(Instant.now().minusSeconds(10 * 60L));

        when(orderRepository.findByStatusInOrderByPickupSlotStartAsc(anyList())).thenReturn(List.of(order));
        when(canteenService.isClosedOn(any())).thenReturn(false);
        when(canteenService.isWithinOperatingHours(any(), any())).thenReturn(true);

        new OrderExpiryScheduler(orderRepository, orderService, canteenService, ZoneId.of("Asia/Kolkata").getId())
                .expireStaleOrders();

        verify(orderService).expireOrder(eq(42L), contains("pickup window has ended"));
    }

    @Test
    void expiresFutureOrderWhenCanteenWindowIsClosed() {
        Order order = new Order();
        order.setId(43L);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setPickupSlotStart(Instant.now().plusSeconds(60 * 60L));
        order.setPickupSlotEnd(Instant.now().plusSeconds(70 * 60L));

        when(orderRepository.findByStatusInOrderByPickupSlotStartAsc(anyList())).thenReturn(List.of(order));
        when(canteenService.isClosedOn(any())).thenReturn(false);
        when(canteenService.isWithinOperatingHours(any(), any())).thenReturn(false);

        new OrderExpiryScheduler(orderRepository, orderService, canteenService, ZoneId.of("Asia/Kolkata").getId())
                .expireStaleOrders();

        verify(orderService).expireOrder(eq(43L), contains("canteen is closed"));
    }

    @Test
    void doesNotExpireFutureOrderWhenOrderingIsPaused() {
        Order order = new Order();
        order.setId(44L);
        order.setStatus(OrderStatus.PLACED);
        order.setPickupSlotStart(Instant.now().plusSeconds(60 * 60L));
        order.setPickupSlotEnd(Instant.now().plusSeconds(70 * 60L));

        when(orderRepository.findByStatusInOrderByPickupSlotStartAsc(anyList())).thenReturn(List.of(order));
        when(canteenService.isClosedOn(any())).thenReturn(false);
        when(canteenService.isWithinOperatingHours(any(), any())).thenReturn(true);

        new OrderExpiryScheduler(orderRepository, orderService, canteenService, ZoneId.of("Asia/Kolkata").getId())
                .expireStaleOrders();

        verify(orderService, never()).expireOrder(anyLong(), anyString());
    }
}

