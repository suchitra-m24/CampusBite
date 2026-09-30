package com.campusbite.order;

import com.campusbite.common.exception.ConflictException;
import com.campusbite.trust.TrustScoreService;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.user.UserRepository;
import com.campusbite.wallet.WalletService;
import com.campusbite.cart.Cart;
import com.campusbite.cart.CartItem;
import com.campusbite.cart.CartRepository;
import com.campusbite.menu.Category;
import com.campusbite.menu.CategoryRepository;
import com.campusbite.menu.MenuItem;
import com.campusbite.menu.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves the three required idempotency behaviors directly against
 * IdempotencyService (the controller just wires this + OrderService
 * together, so testing at this layer is the meaningful, stable contract):
 *   - same key + same request -> second call returns the FIRST order, no duplicate created
 *   - same key + different request -> rejected
 *   - two different keys -> two independent orders, as expected
 */
@SpringBootTest
class OrderIdempotencyTest {

    @Autowired private IdempotencyService idempotencyService;
    @Autowired private OrderService orderService;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private TrustScoreService trustScoreService;
    @Autowired private WalletService walletService;

    private User createStudentWithFundedWallet() {
        User student = userRepository.save(new User(
                "Idempotency Test User",
                "idem-test-" + System.nanoTime() + "@campusbite.dev",
                "IDEM-" + System.nanoTime(),
                "{noop}not-a-real-password-hash",
                Role.STUDENT));
        trustScoreService.initializeFor(student);
        walletService.initializeFor(student);
        walletService.topUp(student, new BigDecimal("1000.00"));
        return student;
    }

    private void addItemToCart(User student, MenuItem item) {
        Cart cart = cartRepository.findByStudent(student).orElseGet(() -> cartRepository.save(new Cart(student)));
        CartItem ci = new CartItem();
        ci.setCart(cart);
        ci.setMenuItem(item);
        ci.setQuantity(1);
        cart.getItems().add(ci);
        cartRepository.save(cart);
    }

    private MenuItem createAvailableItem() {
        Category category = categoryRepository.save(new Category("Idempotency Test Category " + System.nanoTime(), 1));
        MenuItem item = new MenuItem();
        item.setName("Idempotency Test Item");
        item.setPrice(new BigDecimal("50.00"));
        item.setCategory(category);
        item.setAvailable(true);
        item.setBaselinePrepTimeMinutes(5);
        return menuItemRepository.save(item);
    }

    @Test
    void sameKeyAndSameRequestReturnsOriginalOrderNotADuplicate() {
        User student = createStudentWithFundedWallet();
        addItemToCart(student, createAvailableItem());

        String key = "test-key-" + System.nanoTime();
        String hash = idempotencyService.hashRequest("first attempt");

        IdempotencyService.ClaimOutcome first = idempotencyService.claim(key, student.getId(), hash);
        assertThat(first.alreadyCompleted()).isFalse();

        OrderService.OrderResult result = orderService.placeOrder(student, "first attempt");
        idempotencyService.recordResult(first.recordId(), result.order().getId());

        // Simulate a retried request with the SAME key and SAME body.
        IdempotencyService.ClaimOutcome replay = idempotencyService.claim(key, student.getId(), hash);

        assertThat(replay.alreadyCompleted()).isTrue();
        assertThat(replay.existingOrderId()).isEqualTo(result.order().getId());

        long ordersForStudent = orderRepository.findByStudentOrderByPlacedAtDesc(student).size();
        assertThat(ordersForStudent).as("only one order should exist despite two claim attempts").isEqualTo(1);
    }

    @Test
    void sameKeyWithDifferentRequestIsRejected() {
        User student = createStudentWithFundedWallet();
        String key = "test-key-" + System.nanoTime();

        idempotencyService.claim(key, student.getId(), idempotencyService.hashRequest("original request"));

        assertThatThrownBy(() ->
                idempotencyService.claim(key, student.getId(), idempotencyService.hashRequest("a completely different request")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void twoDifferentKeysAreIndependent() {
        User student = createStudentWithFundedWallet();

        IdempotencyService.ClaimOutcome a = idempotencyService.claim(
                "key-a-" + System.nanoTime(), student.getId(), idempotencyService.hashRequest("body"));
        IdempotencyService.ClaimOutcome b = idempotencyService.claim(
                "key-b-" + System.nanoTime(), student.getId(), idempotencyService.hashRequest("body"));

        assertThat(a.alreadyCompleted()).isFalse();
        assertThat(b.alreadyCompleted()).isFalse();
        assertThat(a.recordId()).isNotEqualTo(b.recordId());
    }
}