package com.campusbite.slot.dto;

import java.time.Instant;

public record SlotAllocationResult(
        Instant slotStart,
        Instant slotEnd,
        int ordersAheadInSlot,
        int estimatedPrepMinutes
) {
}
