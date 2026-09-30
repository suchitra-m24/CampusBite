package com.campusbite.rating;

import com.campusbite.common.exception.BadRequestException;
import com.campusbite.order.Order;
import com.campusbite.order.OrderRepository;
import com.campusbite.order.OrderStatus;
import com.campusbite.common.util.TokenGenerator;
import com.campusbite.rating.dto.RatingRequest;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Proves the IDOR fix: student B cannot read or write student A's rating by ID alone. */
@SpringBootTest
class RatingIdorTest {

    @Autowired private RatingService ratingService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    private User createStudent(String label) {
        return userRepository.save(new User(
                label, label + "-" + System.nanoTime() + "@campusbite.dev",
                label + "-" + System.nanoTime(), "{noop}not-a-real-hash", Role.STUDENT));
    }

    @Test
    void studentBCannotReadOrRateStudentAsOrder() {
        User studentA = createStudent("Owner");
        User studentB = createStudent("Attacker");

        Order order = new Order();
        order.setStudent(studentA);
        order.setStatus(OrderStatus.PICKED_UP);
        order.setPlacedAt(Instant.now());
        order.setPickupSlotStart(Instant.now());
        order.setPickupSlotEnd(Instant.now().plusSeconds(600));
        order.setEstimatedPrepMinutes(10);
        order.setPickupToken(TokenGenerator.generatePickupToken());
        order.setTotalAmount(new BigDecimal("100.00"));
        Long orderId = orderRepository.save(order).getId();

        assertThatThrownBy(() -> ratingService.getForOrder(orderId, studentB))
                .isInstanceOf(BadRequestException.class);

        assertThatThrownBy(() -> ratingService.submit(orderId, studentB, new RatingRequest(5, "not mine to rate")))
                .isInstanceOf(BadRequestException.class);
    }
}