package com.campusbite.slot.dto;

import com.campusbite.slot.SlotCapacity;

public record SlotCapacityDto(Long id, int windowStartMinuteOfDay, String label, int maxOrders) {
    public static SlotCapacityDto from(SlotCapacity c) {
        int h = c.getWindowStartMinuteOfDay() / 60;
        int m = c.getWindowStartMinuteOfDay() % 60;
        String label = String.format("%02d:%02d", h, m);
        return new SlotCapacityDto(c.getId(), c.getWindowStartMinuteOfDay(), label, c.getMaxOrders());
    }
}
