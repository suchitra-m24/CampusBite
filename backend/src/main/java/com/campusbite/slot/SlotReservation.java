package com.campusbite.slot;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

/**
 * The authoritative, atomically-updated counter behind slot capacity
 * enforcement. One row per exact pickup-window occurrence (not per
 * time-of-day pattern -- Tuesday's 12:30 and Wednesday's 12:30 are
 * separate rows). Capacity itself isn't stored here; it's looked up from
 * SlotCapacity/the configured default at reservation time and passed into
 * the atomic UPDATE, keeping capacity policy in one place.
 */
@Entity
@Table(name = "slot_reservations", uniqueConstraints = @UniqueConstraint(columnNames = "window_start"))
public class SlotReservation extends BaseEntity {

    @Column(name = "window_start", nullable = false)
    private Instant windowStart;

    @Column(nullable = false)
    private int reservedCount = 0;

    public SlotReservation() {
    }

    public SlotReservation(Instant windowStart) {
        this.windowStart = windowStart;
    }

    public Instant getWindowStart() {
        return windowStart;
    }

    public int getReservedCount() {
        return reservedCount;
    }
}