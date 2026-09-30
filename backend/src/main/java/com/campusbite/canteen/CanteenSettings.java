package com.campusbite.canteen;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalTime;

/** Singleton configuration for the one campus canteen. */
@Entity
@Table(name = "canteen_settings")
public class CanteenSettings extends BaseEntity {
    @Column(nullable = false)
    private String canteenName = "Campus Main Canteen";

    @Column(nullable = false)
    private LocalTime openingTime = LocalTime.of(8, 0);

    @Column(nullable = false)
    private LocalTime closingTime = LocalTime.of(18, 0);

    @Column(nullable = false)
    private boolean orderingEnabled = true;

    public String getCanteenName() { return canteenName; }
    public void setCanteenName(String canteenName) { this.canteenName = canteenName; }
    public LocalTime getOpeningTime() { return openingTime; }
    public void setOpeningTime(LocalTime openingTime) { this.openingTime = openingTime; }
    public LocalTime getClosingTime() { return closingTime; }
    public void setClosingTime(LocalTime closingTime) { this.closingTime = closingTime; }
    public boolean isOrderingEnabled() { return orderingEnabled; }
    public void setOrderingEnabled(boolean orderingEnabled) { this.orderingEnabled = orderingEnabled; }
}
