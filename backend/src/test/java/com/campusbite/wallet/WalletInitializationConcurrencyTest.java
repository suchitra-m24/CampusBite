package com.campusbite.wallet;

import com.campusbite.user.Role;
import com.campusbite.user.User;
import com.campusbite.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves that concurrent first access creates exactly one wallet and exactly
 * one signup-bonus ledger entry. The wallet row is established atomically
 * with INSERT IGNORE against the UNIQUE(student_id) constraint.
 */
@SpringBootTest
class WalletInitializationConcurrencyTest {

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository transactionRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void concurrentFirstAccessCreatesOneWalletAndOneSignupBonus() throws Exception {
        User student = userRepository.save(new User(
                "Wallet Init Test User",
                "wallet-init-" + System.nanoTime() + "@campusbite.dev",
                "WALLET-" + System.nanoTime(),
                "{noop}not-a-real-password-hash",
                Role.STUDENT));

        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        List<Throwable> failures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                try {
                    start.await();
                    walletService.getOrCreate(student);
                } catch (Throwable t) {
                    synchronized (failures) {
                        failures.add(t);
                    }
                } finally {
                    done.countDown();
                }
            });
        }

        start.countDown();
        assertThat(done.await(15, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        assertThat(failures).isEmpty();
        Wallet wallet = walletRepository.findByStudent(student).orElseThrow();
        assertThat(wallet.getBalance()).isEqualByComparingTo("200.00");

        long signupBonuses = transactionRepository.findByWalletOrderByCreatedAtDesc(wallet)
                .stream()
                .filter(t -> t.getType() == WalletTransaction.Type.SIGNUP_BONUS)
                .count();
        assertThat(signupBonuses).isEqualTo(1);
    }
}
