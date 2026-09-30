package com.campusbite.slot;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves slots can't be overbooked under real concurrent load: a slot has
 * capacity 15 and already holds 14 reservations, then two threads try to
 * claim the last spot at the same instant. Exactly one must succeed -- a
 * naive "SELECT COUNT(*) then compare" implementation could let both
 * threads see 14 < 15 and both succeed, landing at 16/15.
 */
@SpringBootTest
class SlotReservationConcurrencyTest {

    @Autowired
    private SlotReservationService slotReservationService;

    @Autowired
    private SlotReservationRepository slotReservationRepository;

    @Test
    void twoSimultaneousReservationsForTheLastSpotAllowExactlyOneToSucceed() throws InterruptedException {
        Instant windowStart = Instant.now().plusSeconds(3600 + System.nanoTime() % 10000); // unique-ish window per run
        int capacity = 15;

        // Pre-fill to 14/15 -- one spot left.
        for (int i = 0; i < 14; i++) {
            boolean reserved = slotReservationService.tryReserve(windowStart, capacity);
            assertThat(reserved).as("pre-fill reservation %d should succeed", i).isTrue();
        }

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    startLatch.await();
                    if (slotReservationService.tryReserve(windowStart, capacity)) {
                        successCount.incrementAndGet();
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finishedInTime = doneLatch.await(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finishedInTime).as("both threads should complete within 15s").isTrue();
        assertThat(successCount.get()).as("exactly one of the two should win the last spot").isEqualTo(1);

        int finalCount = slotReservationRepository.findByWindowStart(windowStart)
                .map(SlotReservation::getReservedCount).orElseThrow();
        assertThat(finalCount).as("capacity must never be exceeded").isEqualTo(capacity);
    }
}