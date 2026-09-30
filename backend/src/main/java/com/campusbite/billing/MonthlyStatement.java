package com.campusbite.billing;

import com.campusbite.common.BaseEntity;
import com.campusbite.user.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "monthly_statements", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "year", "month"}))
public class MonthlyStatement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int year;

    @Column(nullable = false)
    private int month;

    @Column(nullable = false)
    private int orderCount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private boolean settled = false;

    @Column
    private Instant settledAt;

    public MonthlyStatement() {
    }

    public MonthlyStatement(User user, int year, int month, int orderCount, BigDecimal totalAmount) {
        this.user = user;
        this.year = year;
        this.month = month;
        this.orderCount = orderCount;
        this.totalAmount = totalAmount;
    }

    public User getUser() { return user; }
    public int getYear() { return year; }
    public int getMonth() { return month; }
    public int getOrderCount() { return orderCount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public boolean isSettled() { return settled; }
    public void setSettled(boolean settled) { this.settled = settled; }
    public Instant getSettledAt() { return settledAt; }
    public void setSettledAt(Instant settledAt) { this.settledAt = settledAt; }
}