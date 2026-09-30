package com.campusbite.wallet;

import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the wallet debit is safe under real concurrent load: ten threads
 * each try to spend ₹100 from a ₹500 balance at the same instant. A naive
 * read-check-write implementation could let all ten succeed (each reads
 * ₹500 before any of them writes). The atomic conditional UPDATE this
 * service uses must allow EXACTLY five to succeed, with the final balance
 * landing at exactly ₹0 -- never negative.
 *
 * Runs against the same MySQL instance configured in application.yml (no
 * Testcontainers here -- not justified for a single scenario like this;
 * would be worth introducing if the integration test suite grows large
 * enough that isolating it from a shared dev database becomes valuable).
 */
@SpringBootTest
class WalletConcurrencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void tenConcurrentDebitsAgainstFiveHundredRupeesAllowExactlyFiveToSucceed() throws InterruptedException {
        User student = userRepository.save(new User(
                "Concurrency Test User",
                "concurrency-test-" + System.nanoTime() + "@campusbite.dev",
                "CONC-" + System.nanoTime(),
                "{noop}not-a-real-password-hash",
                Role.STUDENT));

        walletRepository.save(new Wallet(student, new BigDecimal("500.00")));

        int threadCount = 10;
        BigDecimal debitAmount = new BigDecimal("100.00");

        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            futures.add(pool.submit(() -> {
                try {
                    startLatch.await();
                    walletService.debitForOrder(student, debitAmount, "concurrency test debit");
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failureCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown(); // release all ten threads at effectively the same instant
        boolean finishedInTime = doneLatch.await(15, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(finishedInTime).as("all threads should complete within 15s").isTrue();

        Wallet finalWallet = walletRepository.findByStudent(student).orElseThrow();

        assertThat(successCount.get()).isEqualTo(5);
        assertThat(failureCount.get()).isEqualTo(5);
        assertThat(finalWallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}