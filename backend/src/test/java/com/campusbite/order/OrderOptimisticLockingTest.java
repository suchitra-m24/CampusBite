package com.campusbite.order;

import com.campusbite.common.util.TokenGenerator;
import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves optimistic locking actually prevents a lost-update: two threads
 * each load the SAME order (same version), one tries to cancel it while
 * the other tries to confirm it, both attempt to save at effectively the
 * same instant. Without @Version, whichever save happens last would
 * silently win and the other change would vanish with no error -- e.g. a
 * student's cancellation could be silently erased by staff confirming a
 * split second later, or vice versa. With @Version, exactly one save must
 * succeed and the other must fail loudly with a conflict.
 */
@SpringBootTest
class OrderOptimisticLockingTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void simultaneousCancelAndConfirmOnSameOrderOnlyOneSucceeds() throws Exception {
        User student = userRepository.save(new User(
                "Optimistic Lock Test User",
                "optlock-test-" + System.nanoTime() + "@campusbite.dev",
                "OPTLOCK-" + System.nanoTime(),
                "{noop}not-a-real-password-hash",
                Role.STUDENT));

        Order order = new Order();
        order.setStudent(student);
        order.setStatus(OrderStatus.PLACED);
        order.setPlacedAt(Instant.now());
        order.setPickupSlotStart(Instant.now().plusSeconds(600));
        order.setPickupSlotEnd(Instant.now().plusSeconds(1200));
        order.setEstimatedPrepMinutes(10);
        order.setPickupToken(TokenGenerator.generatePickupToken());
        order.setTotalAmount(new BigDecimal("100.00"));
        Long orderId = orderRepository.save(order).getId();

        TransactionTemplate tx = new TransactionTemplate(transactionManager);

        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch releaseWrites = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Runnable cancelAttempt = () -> {
            try {
                tx.executeWithoutResult(status -> {
                    Order o = orderRepository.findById(orderId).orElseThrow();
                    bothLoaded.countDown();
                    awaitQuietly(releaseWrites);
                    o.setStatus(OrderStatus.CANCELLED);
                    o.setCancelledAt(Instant.now());
                    orderRepository.saveAndFlush(o);
                });
                successCount.incrementAndGet();
            } catch (OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            }
        };

        Runnable confirmAttempt = () -> {
            try {
                tx.executeWithoutResult(status -> {
                    Order o = orderRepository.findById(orderId).orElseThrow();
                    bothLoaded.countDown();
                    awaitQuietly(releaseWrites);
                    o.setStatus(OrderStatus.CONFIRMED);
                    o.setConfirmedAt(Instant.now());
                    orderRepository.saveAndFlush(o);
                });
                successCount.incrementAndGet();
            } catch (OptimisticLockingFailureException e) {
                conflictCount.incrementAndGet();
            }
        };

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> f1 = pool.submit(cancelAttempt);
        Future<?> f2 = pool.submit(confirmAttempt);

        // Wait until BOTH threads have loaded the same version of the order
        // before either is allowed to write -- this is what guarantees a
        // genuine conflict rather than a lucky sequential pass.
        boolean bothReady = bothLoaded.await(10, TimeUnit.SECONDS);
        assertThat(bothReady).as("both threads should have loaded the order before either writes").isTrue();
        releaseWrites.countDown();

        f1.get(15, TimeUnit.SECONDS);
        f2.get(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(successCount.get()).as("exactly one of the two conflicting updates should succeed").isEqualTo(1);
        assertThat(conflictCount.get()).as("the other must fail loudly, not silently overwrite").isEqualTo(1);

        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(finalOrder.getStatus()).isIn(OrderStatus.CANCELLED, OrderStatus.CONFIRMED);
    }

    private void awaitQuietly(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}