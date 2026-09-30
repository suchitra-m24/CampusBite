package com.campusbite.slot.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SlotCapacityRequest(
        @NotNull @Min(0) Integer windowStartMinuteOfDay,
        @NotNull @Min(1) Integer maxOrders
) {
}
