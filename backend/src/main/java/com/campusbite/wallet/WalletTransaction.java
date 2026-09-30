package com.campusbite.wallet;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "wallet_transactions")
public class WalletTransaction extends BaseEntity {

    public enum Type { TOP_UP, ORDER_PAYMENT, REFUND, SIGNUP_BONUS }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount; // positive = credit, negative = debit

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal balanceAfter;

    @Column
    private String note;

    @Column(name = "order_id")
    private Long orderId;

    public WalletTransaction() {
    }

    public WalletTransaction(Wallet wallet, Type type, BigDecimal amount, BigDecimal balanceAfter, String note) {
        this(wallet, type, amount, balanceAfter, note, null);
    }

    public WalletTransaction(Wallet wallet, Type type, BigDecimal amount, BigDecimal balanceAfter, String note, Long orderId) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.note = note;
        this.orderId = orderId;
    }

    public Wallet getWallet() {
        return wallet;
    }

    public Type getType() {
        return type;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BigDecimal getBalanceAfter() {
        return balanceAfter;
    }

    public String getNote() {
        return note;
    }

    public Long getOrderId() {
        return orderId;
    }
}
