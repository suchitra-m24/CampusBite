package com.campusbite.kitchenload;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A point-in-time reading of how busy the kitchen is, recalculated on a
 * schedule. History of these rows is what lets the admin analytics
 * dashboard chart peak hours.
 */
@Entity
@Table(name = "kitchen_load_snapshots")
public class KitchenLoadSnapshot extends BaseEntity {

    @Column(nullable = false)
    private int activeOrderCount;

    @Column(nullable = false)
    private int backlogMinutes;

    @Column(nullable = false)
    private int loadPercentage;

    @Column(nullable = false)
    private String level; // LOW, MODERATE, BUSY, VERY_BUSY

    public KitchenLoadSnapshot() {
    }

    public KitchenLoadSnapshot(int activeOrderCount, int backlogMinutes, int loadPercentage, String level) {
        this.activeOrderCount = activeOrderCount;
        this.backlogMinutes = backlogMinutes;
        this.loadPercentage = loadPercentage;
        this.level = level;
    }

    public int getActiveOrderCount() {
        return activeOrderCount;
    }

    public int getBacklogMinutes() {
        return backlogMinutes;
    }

    public int getLoadPercentage() {
        return loadPercentage;
    }

    public String getLevel() {
        return level;
    }
}
