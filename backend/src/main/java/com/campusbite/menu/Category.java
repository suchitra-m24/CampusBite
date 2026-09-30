package com.campusbite.menu;

import com.campusbite.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "categories")
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column
    private int displayOrder;

    /**
     * Optional time-of-day window (minutes since midnight, 0-1439) during
     * which items in this category are shown to students -- e.g. Breakfast
     * = 360 (6:00 AM) to 660 (11:00 AM). Either field left null means the
     * category has no time restriction and is always visible.
     */
    @Column
    private Integer availableFromMinuteOfDay;

    @Column
    private Integer availableToMinuteOfDay;

    public Category() {
    }

    public Category(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Integer getAvailableFromMinuteOfDay() {
        return availableFromMinuteOfDay;
    }

    public void setAvailableFromMinuteOfDay(Integer availableFromMinuteOfDay) {
        this.availableFromMinuteOfDay = availableFromMinuteOfDay;
    }

    public Integer getAvailableToMinuteOfDay() {
        return availableToMinuteOfDay;
    }

    public void setAvailableToMinuteOfDay(Integer availableToMinuteOfDay) {
        this.availableToMinuteOfDay = availableToMinuteOfDay;
    }
}