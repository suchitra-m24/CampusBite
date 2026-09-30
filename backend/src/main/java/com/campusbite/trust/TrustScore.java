package com.campusbite.trust;

import com.campusbite.common.BaseEntity;
import com.campusbite.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Tracks a student's reliability. Starts every student at 100. No-shows
 * (an order that reached READY and was never picked up within the grace
 * window) dock points and, below a threshold, temporarily restrict how
 * many active slots a student may hold at once.
 */
@Entity
@Table(name = "trust_scores")
public class TrustScore extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(nullable = false)
    private int score = 100;

    @Column(nullable = false)
    private int totalOrders = 0;

    @Column(nullable = false)
    private int noShowCount = 0;

    @Column(nullable = false)
    private int cancelledLateCount = 0;

    public TrustScore() {
    }

    public TrustScore(User user) {
        this.user = user;
    }

    public static final int NO_SHOW_PENALTY = 15;
    public static final int LATE_CANCEL_PENALTY = 5;
    public static final int SUCCESSFUL_PICKUP_BONUS = 2;
    public static final int MIN_SCORE = 0;
    public static final int MAX_SCORE = 100;

    /** Below this score, a student may only hold one active slot at a time. */
    public static final int RESTRICTED_THRESHOLD = 50;

    public void recordNoShow() {
        noShowCount++;
        totalOrders++;
        score = Math.max(MIN_SCORE, score - NO_SHOW_PENALTY);
    }

    public void recordLateCancel() {
        cancelledLateCount++;
        score = Math.max(MIN_SCORE, score - LATE_CANCEL_PENALTY);
    }

    public void recordSuccessfulPickup() {
        totalOrders++;
        score = Math.min(MAX_SCORE, score + SUCCESSFUL_PICKUP_BONUS);
    }

    public boolean isRestricted() {
        return score < RESTRICTED_THRESHOLD;
    }

    public User getUser() {
        return user;
    }

    public int getScore() {
        return score;
    }

    public int getTotalOrders() {
        return totalOrders;
    }

    public int getNoShowCount() {
        return noShowCount;
    }

    public int getCancelledLateCount() {
        return cancelledLateCount;
    }
}
