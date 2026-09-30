package com.campusbite.order;

import com.campusbite.common.BaseEntity;
import com.campusbite.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "item_index")
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, unique = true)
    private String pickupToken;

    @Column(nullable = false)
    private Instant pickupSlotStart;

    @Column(nullable = false)
    private Instant pickupSlotEnd;

    @Column(nullable = false)
    private int estimatedPrepMinutes;

    /** Filled in once the order reaches READY: readyAt - confirmedAt, in minutes. Feeds prep-time prediction. */
    @Column
    private Integer actualPrepMinutes;

    @Column(length = 300)
    private String specialInstructions;

    // ---- status timestamps: these power notifications, analytics and prep-time prediction ----
    @Column(nullable = false)
    private Instant placedAt;

    @Column
    private Instant confirmedAt;

    @Column
    private Instant preparingAt;

    @Column
    private Instant readyAt;

    @Column
    private Instant pickedUpAt;

    @Column
    private Instant cancelledAt;

    @Column(length = 300)
    private String cancelReason;

    @Column(nullable = false)
    private boolean delayNotified = false;

    /**
     * Optimistic locking: every UPDATE to this row implicitly includes
     * "WHERE version = ?", and Hibernate bumps it on every successful save.
     * If two requests (e.g. a student cancelling while staff confirms the
     * same order) both load the order, then both try to save, the second
     * one's WHERE clause matches zero rows -- Hibernate detects that and
     * throws instead of silently overwriting the first change.
     */
    @jakarta.persistence.Version
    private Long version;

    public Order() {
    }

    // getters / setters

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getPickupToken() {
        return pickupToken;
    }

    public void setPickupToken(String pickupToken) {
        this.pickupToken = pickupToken;
    }

    public Instant getPickupSlotStart() {
        return pickupSlotStart;
    }

    public void setPickupSlotStart(Instant pickupSlotStart) {
        this.pickupSlotStart = pickupSlotStart;
    }

    public Instant getPickupSlotEnd() {
        return pickupSlotEnd;
    }

    public void setPickupSlotEnd(Instant pickupSlotEnd) {
        this.pickupSlotEnd = pickupSlotEnd;
    }

    public int getEstimatedPrepMinutes() {
        return estimatedPrepMinutes;
    }

    public void setEstimatedPrepMinutes(int estimatedPrepMinutes) {
        this.estimatedPrepMinutes = estimatedPrepMinutes;
    }

    public Integer getActualPrepMinutes() {
        return actualPrepMinutes;
    }

    public void setActualPrepMinutes(Integer actualPrepMinutes) {
        this.actualPrepMinutes = actualPrepMinutes;
    }

    public String getSpecialInstructions() {
        return specialInstructions;
    }

    public void setSpecialInstructions(String specialInstructions) {
        this.specialInstructions = specialInstructions;
    }

    public Instant getPlacedAt() {
        return placedAt;
    }

    public void setPlacedAt(Instant placedAt) {
        this.placedAt = placedAt;
    }

    public Instant getConfirmedAt() {
        return confirmedAt;
    }

    public void setConfirmedAt(Instant confirmedAt) {
        this.confirmedAt = confirmedAt;
    }

    public Instant getPreparingAt() {
        return preparingAt;
    }

    public void setPreparingAt(Instant preparingAt) {
        this.preparingAt = preparingAt;
    }

    public Instant getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(Instant readyAt) {
        this.readyAt = readyAt;
    }

    public Instant getPickedUpAt() {
        return pickedUpAt;
    }

    public void setPickedUpAt(Instant pickedUpAt) {
        this.pickedUpAt = pickedUpAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }

    public boolean isDelayNotified() {
        return delayNotified;
    }

    public void setDelayNotified(boolean delayNotified) {
        this.delayNotified = delayNotified;
    }

    public Long getVersion() {
        return version;
    }
}
