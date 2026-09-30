package com.campusbite.broadcast;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/**
 * A single admin-authored banner shown across student pages, e.g. "Kitchen
 * closed 2-3pm for cleaning". Only one is ever active at a time -- setting
 * a new one implicitly replaces whatever was showing before.
 */
@Entity
@Table(name = "broadcast_messages")
public class BroadcastMessage extends BaseEntity {

    @Column(nullable = false, length = 300)
    private String message;

    @Column(nullable = false)
    private boolean active = false;

    public BroadcastMessage() {
    }

    public BroadcastMessage(String message, boolean active) {
        this.message = message;
        this.active = active;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
