package com.campusbite.slot;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * Admin-configured capacity for a recurring window of the day, e.g.
 * "12:00-12:10 -> max 25 orders" for the lunch rush vs "15:00-15:10 -> max 10"
 * for a quiet afternoon. windowStartMinuteOfDay is minutes since midnight
 * (0-1439) and must align to the global slot window size.
 */
@Entity
@Table(name = "slot_capacities")
public class SlotCapacity extends BaseEntity {

    @Column(nullable = false)
    private int windowStartMinuteOfDay;

    @Column(nullable = false)
    private int maxOrders;

    public SlotCapacity() {
    }

    public SlotCapacity(int windowStartMinuteOfDay, int maxOrders) {
        this.windowStartMinuteOfDay = windowStartMinuteOfDay;
        this.maxOrders = maxOrders;
    }

    public int getWindowStartMinuteOfDay() {
        return windowStartMinuteOfDay;
    }

    public void setWindowStartMinuteOfDay(int windowStartMinuteOfDay) {
        this.windowStartMinuteOfDay = windowStartMinuteOfDay;
    }

    public int getMaxOrders() {
        return maxOrders;
    }

    public void setMaxOrders(int maxOrders) {
        this.maxOrders = maxOrders;
    }
}
