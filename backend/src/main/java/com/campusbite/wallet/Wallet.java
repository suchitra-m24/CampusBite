package com.campusbite.wallet;

import com.campusbite.common.BaseEntity;
import com.campusbite.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/** A simulated wallet -- no real payment gateway is wired up, this stands in for one. */
@Entity
@Table(name = "wallets")
public class Wallet extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false, unique = true)
    private User student;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    public Wallet() {
    }

    public Wallet(User student, BigDecimal balance) {
        this.student = student;
        this.balance = balance;
    }

    public User getStudent() {
        return student;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
