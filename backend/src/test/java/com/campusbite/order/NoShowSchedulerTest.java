package com.campusbite.order;

import com.campusbite.user.Role;
import com.campusbite.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoShowSchedulerTest {

    @Mock OrderRepository orderRepository;
    @Mock OrderService orderService;

    @Test
    void usesPickupSlotEndPlusGraceNotReadyAt() {
        User student = new User("Student", "s@example.com", "S001", "hash", Role.STUDENT);
        Order order = new Order();
        order.setId(7L);
        order.setStudent(student);
        order.setStatus(OrderStatus.READY);
        order.setPickupSlotEnd(Instant.now().minusSeconds(30 * 60L));
        order.setReadyAt(Instant.now().minusSeconds(2 * 60L));

        when(orderRepository.findByStatusAndPickupSlotEndBefore(eq(OrderStatus.READY), any()))
                .thenReturn(List.of(order));

        new NoShowScheduler(orderRepository, orderService, 20)
                .detectNoShows();

        verify(orderService).markNoShow(7L);
    }
}
