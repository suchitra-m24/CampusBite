package com.campusbite.wallet;

import com.campusbite.common.exception.BadRequestException;
import com.campusbite.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;

/**
 * Simulated payment gateway: no real money moves, but the flow (balance
 * check, debit, credit, transaction ledger) mirrors how a real one would
 * plug in, so swapping in Razorpay/Stripe later only touches this class.
 *
 * Concurrency: debits use an atomic conditional UPDATE (see
 * WalletRepository.debitIfSufficient) rather than a read-check-write
 * sequence, so the balance can never go negative under concurrent
 * requests -- see WalletConcurrencyTest for a test proving this directly.
 */
@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository transactionRepository;
    private final BigDecimal signupBonus;
    private final EntityManager entityManager;

    public WalletService(WalletRepository walletRepository,
                         WalletTransactionRepository transactionRepository,
                         @Value("${campusbite.wallet.signup-bonus}") BigDecimal signupBonus,
                         EntityManager entityManager) {
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.signupBonus = signupBonus;
        this.entityManager = entityManager;
    }

    @Transactional
    public Wallet initializeFor(User student) {
        int created = walletRepository.createIfAbsent(student.getId(), signupBonus);
        Wallet wallet = walletRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalStateException("Wallet could not be initialized for user " + student.getId()));

        if (created == 1) {
            transactionRepository.save(new WalletTransaction(
                    wallet, WalletTransaction.Type.SIGNUP_BONUS, signupBonus, signupBonus, "Welcome bonus"));
        }
        return wallet;
    }

    @Transactional
    public Wallet getOrCreate(User student) {
        return walletRepository.findByStudent(student).orElseGet(() -> initializeFor(student));
    }

    @Transactional
    public void debitForOrder(User student, BigDecimal amount, String note) {
        debitForOrder(student, amount, note, null);
    }

    @Transactional
    public void debitForOrder(User student, BigDecimal amount, String note, Long orderId) {
        // Ensures the wallet row exists (no-op if it already does) -- the
        // actual balance decision happens entirely in the atomic UPDATE below,
        // never from this read.
        getOrCreate(student);

        int rowsUpdated = walletRepository.debitIfSufficient(student.getId(), amount);
        if (rowsUpdated == 0) {
            throw new BadRequestException("Insufficient wallet balance. Please top up to place this order.");
        }

        Wallet wallet = walletRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalStateException("Wallet vanished mid-transaction for user " + student.getId()));
        entityManager.refresh(wallet);
        transactionRepository.save(new WalletTransaction(
                wallet, WalletTransaction.Type.ORDER_PAYMENT, amount.negate(), wallet.getBalance(), note, orderId));
    }

    @Transactional
    public void refund(User student, BigDecimal amount, String note) {
        refund(student, amount, note, null);
    }

    @Transactional
    public void refund(User student, BigDecimal amount, String note, Long orderId) {
        getOrCreate(student);
        walletRepository.credit(student.getId(), amount);

        Wallet wallet = walletRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalStateException("Wallet vanished mid-transaction for user " + student.getId()));
        entityManager.refresh(wallet);
        transactionRepository.save(new WalletTransaction(
                wallet, WalletTransaction.Type.REFUND, amount, wallet.getBalance(), note, orderId));
    }

    @Transactional
    public Wallet topUp(User student, BigDecimal amount) {
        getOrCreate(student);
        walletRepository.credit(student.getId(), amount);

        Wallet wallet = walletRepository.findByStudent(student)
                .orElseThrow(() -> new IllegalStateException("Wallet vanished mid-transaction for user " + student.getId()));
        entityManager.refresh(wallet);
        transactionRepository.save(new WalletTransaction(
                wallet, WalletTransaction.Type.TOP_UP, amount, wallet.getBalance(), "Wallet top-up"));
        return wallet;
    }
}