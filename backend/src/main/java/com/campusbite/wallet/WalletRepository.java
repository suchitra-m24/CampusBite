package com.campusbite.wallet;

import com.campusbite.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    Optional<Wallet> findByStudent(User student);

    /**
     * Atomically creates the wallet with the signup bonus if it does not already exist.
     * MySQL/InnoDB UNIQUE(student_id) makes concurrent first access safe; INSERT IGNORE
     * turns a concurrent duplicate into a no-op instead of a unique-key exception.
     * Returns 1 only when this call created the wallet.
     */
    @Modifying(flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO wallets (created_at, updated_at, student_id, balance) " +
                   "SELECT NOW(6), NOW(6), u.id, :bonus FROM users u WHERE u.id = :studentId", nativeQuery = true)
    int createIfAbsent(@Param("studentId") Long studentId, @Param("bonus") BigDecimal bonus);

    /**
     * Atomic, database-level debit: the WHERE clause re-checks the balance
     * at the moment the row lock is acquired, not at some earlier read.
     * Under InnoDB, this UPDATE takes a row lock and re-evaluates the
     * condition against the current committed value -- two concurrent
     * debits against the same wallet serialize at the database, so it's
     * impossible for both to succeed if only one has sufficient balance.
     * Returns the number of rows affected: 1 = debited, 0 = insufficient
     * balance (or wallet doesn't exist).
     */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance - :amount WHERE w.student.id = :studentId AND w.balance >= :amount")
    int debitIfSufficient(@Param("studentId") Long studentId, @Param("amount") BigDecimal amount);

    /** Credits never need the balance-check guard, but stay atomic for the same reason. */
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Wallet w SET w.balance = w.balance + :amount WHERE w.student.id = :studentId")
    int credit(@Param("studentId") Long studentId, @Param("amount") BigDecimal amount);
}