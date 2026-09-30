package com.campusbite.order;

import com.campusbite.canteen.CanteenService;
import com.campusbite.cart.CartRepository;
import com.campusbite.common.ws.BroadcastService;
import com.campusbite.kitchenload.KitchenLoadService;
import com.campusbite.push.PushNotificationService;
import com.campusbite.slot.SlotAllocationService;
import com.campusbite.slot.SlotReservationService;
import com.campusbite.trust.TrustScoreService;
import com.campusbite.user.PaymentMode;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.wallet.WalletService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderCancellationPaymentModeTest {

    @Mock OrderRepository orderRepository;
    @Mock CartRepository cartRepository;
    @Mock SlotAllocationService slotAllocationService;
    @Mock PrepTimePredictionService prepTimePredictionService;
    @Mock KitchenLoadService kitchenLoadService;
    @Mock TrustScoreService trustScoreService;
    @Mock WalletService walletService;
    @Mock BroadcastService broadcastService;
    @Mock PushNotificationService pushNotificationService;
    @Mock SlotReservationService slotReservationService;
    @Mock CanteenService canteenService;

    @Test
    void cancellingPostpaidOrderDoesNotCreateWalletRefund() {
        OrderService service = new OrderService(
                orderRepository, cartRepository, slotAllocationService, prepTimePredictionService,
                kitchenLoadService, trustScoreService, walletService, broadcastService,
                pushNotificationService, slotReservationService, 15, canteenService);

        User teacher = new User("Teacher", "teacher@campusbite.dev", "T-001", "hash", Role.TEACHER);
        teacher.setId(100L);
        teacher.setPaymentMode(PaymentMode.POSTPAID);

        Order order = new Order();
        order.setId(200L);
        order.setStudent(teacher);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("120.00"));
        order.setPickupToken("CB1234");
        order.setPickupSlotStart(Instant.now().plusSeconds(3600));
        order.setPickupSlotEnd(order.getPickupSlotStart().plusSeconds(600));

        when(orderRepository.findById(200L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        service.cancel(200L, teacher);

        verify(slotReservationService).release(order.getPickupSlotStart());
        verify(walletService, never()).refund(any(User.class), any(BigDecimal.class), anyString());
        verify(orderRepository).save(order);
    }
}
